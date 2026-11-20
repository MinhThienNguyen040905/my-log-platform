package com.mylog.insight.infrastructure.persistence;

import com.mylog.insight.application.ThinkingPatternStore;
import com.mylog.insight.domain.ThinkingPatternRules;
import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.platform.id.IdGenerator;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Native SQL over V22. Signals are read only for entries that are still current and screened ALLOW, so a
 * deleted, edited or escalated entry stops counting immediately even before the extractor purges it.
 * Transactions are opened by {@code ThinkingPatternService}.
 */
@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JpaThinkingPatternStore implements ThinkingPatternStore {
    static final String NARRATIVE_FIELD = "pattern_recognitions.narrative";
    /** Must match the field the analysis module encrypts quotes with. */
    static final String QUOTE_FIELD = "entry_pattern_signals.quote";
    private static final String CARD_COLUMNS = """
            id, pattern_code, period_start, period_end, status, user_feedback, feedback_reason, feedback_at,
            entry_count, week_count, encrypted_narrative, narrative_iv, narrative_wrapped_key, narrative_key_version,
            generated_by, knowledge_version_id, plan_goal_id, plan_started_at, created_at, updated_at""";

    private final EntityManager em;
    private final SensitiveDataCipher cipher;
    private final IdGenerator ids;

    JpaThinkingPatternStore(EntityManager em, SensitiveDataCipher cipher, IdGenerator ids) {
        this.em = em; this.cipher = cipher; this.ids = ids;
    }

    @Override
    public void lock(UUID userId) {
        em.createNativeQuery("SELECT pg_advisory_xact_lock(hashtextextended(?1,0))")
                .setParameter(1, userId + ":thinking-patterns").getSingleResult();
    }

    @Override
    public List<ThinkingPatternRules.SignalPoint> signals(UUID userId, LocalDate from, LocalDate to, String taxonomyVersion) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT s.id, s.journal_entry_id, s.pattern_code, j.local_date, s.confidence
                FROM entry_pattern_signals s
                JOIN entry_pattern_extractions x ON x.id=s.extraction_id
                JOIN journal_entries j ON j.id=s.journal_entry_id AND j.user_id=s.user_id
                WHERE s.user_id=?1 AND x.taxonomy_version=?2 AND x.content_version=j.content_version
                    AND j.local_date BETWEEN ?3 AND ?4 AND j.deleted_at IS NULL AND j.entry_status='SAVED'
                    AND j.risk_level IN ('NORMAL','LOW')
                ORDER BY j.local_date, s.id
                """).setParameter(1, userId).setParameter(2, taxonomyVersion).setParameter(3, from)
                .setParameter(4, to).getResultList();
        List<ThinkingPatternRules.SignalPoint> out = new ArrayList<>(rows.size());
        for (Object[] row : rows)
            out.add(new ThinkingPatternRules.SignalPoint((UUID) row[0], (UUID) row[1], (String) row[2],
                    (LocalDate) row[3], (BigDecimal) row[4]));
        return out;
    }

    @Override
    public List<ThinkingPatternRules.Existing> existing(UUID userId, String taxonomyVersion) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT id, pattern_code, status, user_feedback, feedback_at, plan_goal_id IS NOT NULL, created_at
                FROM pattern_recognitions WHERE user_id=?1 AND taxonomy_version=?2
                """).setParameter(1, userId).setParameter(2, taxonomyVersion).getResultList();
        List<ThinkingPatternRules.Existing> out = new ArrayList<>(rows.size());
        for (Object[] row : rows)
            out.add(new ThinkingPatternRules.Existing((UUID) row[0], (String) row[1], (String) row[2], (String) row[3],
                    instant(row[4]), Boolean.TRUE.equals(row[5]), instant(row[6])));
        return out;
    }

    @Override
    public Optional<UUID> approvedKnowledgeVersion(String topicCode, String locale, Instant now) {
        @SuppressWarnings("unchecked")
        List<UUID> rows = em.createNativeQuery("""
                SELECT v.id FROM knowledge_versions v JOIN knowledge_items i ON i.id=v.item_id
                WHERE i.topic_code=?1 AND i.locale=?2 AND i.status='ACTIVE' AND v.status='APPROVED'
                    AND v.approved_at<=?3 AND (v.effective_from IS NULL OR v.effective_from<=?3)
                    AND (v.effective_to IS NULL OR v.effective_to>?3)
                ORDER BY v.approved_at DESC LIMIT 1
                """, UUID.class).setParameter(1, topicCode).setParameter(2, locale)
                .setParameter(3, Timestamp.from(now)).getResultList();
        return rows.stream().findFirst();
    }

    @Override
    public UUID create(UUID userId, String taxonomyVersion, Write write, Instant now) {
        UUID id = ids.next();
        var narrative = cipher.encrypt(NARRATIVE_FIELD, userId, id, write.narrative());
        em.createNativeQuery("""
                INSERT INTO pattern_recognitions(id,user_id,pattern_code,taxonomy_version,period_start,period_end,timezone,
                    status,entry_count,week_count,average_confidence,knowledge_version_id,encrypted_narrative,narrative_iv,
                    narrative_wrapped_key,narrative_key_version,generated_by,created_at,updated_at)
                VALUES (?1,?2,?3,?4,?5,?6,?7,'ACTIVE',?8,?9,?10,?11,?12,?13,?14,?15,'TEMPLATE',?16,?16)
                """).setParameter(1, id).setParameter(2, userId).setParameter(3, write.code())
                .setParameter(4, taxonomyVersion).setParameter(5, write.periodStart()).setParameter(6, write.periodEnd())
                .setParameter(7, write.timezone()).setParameter(8, write.stats().entryCount())
                .setParameter(9, write.stats().weekCount()).setParameter(10, write.stats().averageConfidence())
                .setParameter(11, write.knowledgeVersionId()).setParameter(12, narrative.ciphertext())
                .setParameter(13, narrative.iv()).setParameter(14, narrative.wrappedKey())
                .setParameter(15, narrative.keyVersion()).setParameter(16, Timestamp.from(now)).executeUpdate();
        replaceEvidence(id, write.stats());
        return id;
    }

    @Override
    public void update(UUID userId, UUID cardId, Write write, Instant now) {
        var narrative = cipher.encrypt(NARRATIVE_FIELD, userId, cardId, write.narrative());
        em.createNativeQuery("""
                UPDATE pattern_recognitions SET period_start=?3, period_end=?4, timezone=?5, entry_count=?6, week_count=?7,
                    average_confidence=?8, knowledge_version_id=?9, encrypted_narrative=?10, narrative_iv=?11,
                    narrative_wrapped_key=?12, narrative_key_version=?13, generated_by='TEMPLATE', model=NULL, updated_at=?14
                WHERE id=?1 AND user_id=?2 AND status='ACTIVE'
                """).setParameter(1, cardId).setParameter(2, userId).setParameter(3, write.periodStart())
                .setParameter(4, write.periodEnd()).setParameter(5, write.timezone())
                .setParameter(6, write.stats().entryCount()).setParameter(7, write.stats().weekCount())
                .setParameter(8, write.stats().averageConfidence()).setParameter(9, write.knowledgeVersionId())
                .setParameter(10, narrative.ciphertext()).setParameter(11, narrative.iv())
                .setParameter(12, narrative.wrappedKey()).setParameter(13, narrative.keyVersion())
                .setParameter(14, Timestamp.from(now)).executeUpdate();
        replaceEvidence(cardId, write.stats());
    }

    private void replaceEvidence(UUID cardId, ThinkingPatternRules.Stats stats) {
        em.createNativeQuery("DELETE FROM pattern_recognition_evidence WHERE recognition_id=?1")
                .setParameter(1, cardId).executeUpdate();
        Set<UUID> quotes = new HashSet<>(stats.quoteSignalIds());
        for (var point : stats.evidence())
            em.createNativeQuery("""
                    INSERT INTO pattern_recognition_evidence(recognition_id,signal_id,shown_as_quote) VALUES (?1,?2,?3)
                    """).setParameter(1, cardId).setParameter(2, point.signalId())
                    .setParameter(3, quotes.contains(point.signalId())).executeUpdate();
    }

    @Override
    public void supersede(UUID userId, List<UUID> cardIds, Instant now) {
        for (UUID id : cardIds)
            em.createNativeQuery("""
                    UPDATE pattern_recognitions SET status='SUPERSEDED', superseded_at=?3, updated_at=?3
                    WHERE id=?1 AND user_id=?2 AND status='ACTIVE'
                    """).setParameter(1, id).setParameter(2, userId).setParameter(3, Timestamp.from(now)).executeUpdate();
    }

    @Override
    public List<Card> active(UUID userId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("SELECT " + CARD_COLUMNS + """
                 FROM pattern_recognitions WHERE user_id=?1 AND status='ACTIVE' ORDER BY created_at, id
                """).setParameter(1, userId).getResultList();
        return rows.stream().map(row -> card(userId, row)).toList();
    }

    @Override
    public Optional<Card> find(UUID userId, UUID cardId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("SELECT " + CARD_COLUMNS + """
                 FROM pattern_recognitions WHERE id=?1 AND user_id=?2
                """).setParameter(1, cardId).setParameter(2, userId).getResultList();
        return rows.stream().findFirst().map(row -> card(userId, row));
    }

    @Override
    public boolean feedback(UUID userId, UUID cardId, String feedback, String reason, Instant now) {
        boolean retire = "NOT_REALLY".equals(feedback);
        return em.createNativeQuery("""
                UPDATE pattern_recognitions SET user_feedback=?3, feedback_reason=?4, feedback_at=?5, updated_at=?5,
                    status=CASE WHEN ?6 THEN 'SUPERSEDED' ELSE status END,
                    superseded_at=CASE WHEN ?6 THEN ?5 ELSE superseded_at END
                WHERE id=?1 AND user_id=?2 AND status='ACTIVE'
                """).setParameter(1, cardId).setParameter(2, userId).setParameter(3, feedback)
                .setParameter(4, reason).setParameter(5, Timestamp.from(now)).setParameter(6, retire)
                .executeUpdate() == 1;
    }

    @Override
    public boolean hide(UUID userId, UUID cardId, Instant now) {
        return em.createNativeQuery("""
                UPDATE pattern_recognitions SET status='HIDDEN', updated_at=?3 WHERE id=?1 AND user_id=?2 AND status='ACTIVE'
                """).setParameter(1, cardId).setParameter(2, userId).setParameter(3, Timestamp.from(now))
                .executeUpdate() == 1;
    }

    @Override
    public boolean attachPlan(UUID userId, UUID cardId, UUID goalId, Instant now) {
        return em.createNativeQuery("""
                UPDATE pattern_recognitions SET plan_goal_id=?3, plan_started_at=?4, updated_at=?4
                WHERE id=?1 AND user_id=?2 AND status='ACTIVE' AND plan_goal_id IS NULL
                """).setParameter(1, cardId).setParameter(2, userId).setParameter(3, goalId)
                .setParameter(4, Timestamp.from(now)).executeUpdate() == 1;
    }

    @Override
    public List<Map<String, Object>> exportAll(UUID userId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT id, pattern_code, taxonomy_version, period_start, period_end, status, user_feedback, feedback_reason,
                    feedback_at, entry_count, week_count, plan_started_at, created_at
                FROM pattern_recognitions WHERE user_id=?1 ORDER BY created_at, id
                """).setParameter(1, userId).getResultList();
        List<Map<String, Object>> out = new ArrayList<>(rows.size());
        String[] names = {"id", "code", "taxonomyVersion", "periodStart", "periodEnd", "status", "feedback",
                "feedbackReason", "feedbackAt", "entryCount", "weekCount", "planStartedAt", "createdAt"};
        for (Object[] row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            for (int i = 0; i < names.length; i++) {
                Object value = row[i];
                item.put(names[i], value instanceof Timestamp || value instanceof OffsetDateTime
                        || value instanceof ZonedDateTime ? instant(value) : value);
            }
            out.add(item);
        }
        return out;
    }

    private Card card(UUID userId, Object[] row) {
        UUID id = (UUID) row[0];
        String narrative = cipher.decrypt(NARRATIVE_FIELD, userId, id, new SensitiveDataCipher.Encrypted(
                (byte[]) row[10], (byte[]) row[11], (byte[]) row[12], (String) row[13]));
        return new Card(id, (String) row[1], (LocalDate) row[2], (LocalDate) row[3], (String) row[4], (String) row[5],
                (String) row[6], instant(row[7]), ((Number) row[8]).intValue(), ((Number) row[9]).intValue(), narrative,
                (String) row[14], (UUID) row[15], (UUID) row[16], instant(row[17]), instant(row[18]), instant(row[19]),
                quotes(userId, id));
    }

    /** Shown quotes of signals that still exist (a deleted entry takes its quote with it). */
    private List<Quote> quotes(UUID userId, UUID cardId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT s.id, s.encrypted_quote, s.quote_iv, s.quote_wrapped_key, s.quote_key_version, j.local_date
                FROM pattern_recognition_evidence e
                JOIN entry_pattern_signals s ON s.id=e.signal_id AND s.user_id=?2
                JOIN journal_entries j ON j.id=s.journal_entry_id AND j.deleted_at IS NULL
                WHERE e.recognition_id=?1 AND e.shown_as_quote
                ORDER BY j.local_date DESC, s.id
                """).setParameter(1, cardId).setParameter(2, userId).getResultList();
        List<Quote> out = new ArrayList<>(rows.size());
        for (Object[] row : rows)
            out.add(new Quote(cipher.decrypt(QUOTE_FIELD, userId, (UUID) row[0], new SensitiveDataCipher.Encrypted(
                    (byte[]) row[1], (byte[]) row[2], (byte[]) row[3], (String) row[4])), (LocalDate) row[5]));
        return out;
    }

    private static Instant instant(Object value) {
        return switch (value) {
            case null -> null;
            case Instant i -> i;
            case Timestamp t -> t.toInstant();
            case OffsetDateTime o -> o.toInstant();
            case ZonedDateTime z -> z.toInstant();
            default -> throw new IllegalStateException("Unexpected timestamp type " + value.getClass().getSimpleName());
        };
    }
}
