package com.mylog.insight.infrastructure.persistence;

import com.mylog.insight.infrastructure.persistence.entity.Insight;
import com.mylog.insight.infrastructure.persistence.entity.InsightEvidence;

import com.mylog.insight.application.InsightStore;
import com.mylog.insight.application.query.InsightEvidenceView;
import com.mylog.insight.application.query.InsightPage;
import com.mylog.insight.application.query.InsightView;
import com.mylog.insight.domain.MoodSleepAssociation;
import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.platform.id.IdGenerator;
import com.mylog.platform.web.InvalidRequestException;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JpaInsightStore implements InsightStore {
    private static final String VERSION = "sleep-mood-pearson-v1";
    private final EntityManager em;
    private final IdGenerator ids;
    private final SensitiveDataCipher cipher;

    JpaInsightStore(EntityManager em, IdGenerator ids, SensitiveDataCipher cipher) {
        this.em = em; this.ids = ids; this.cipher = cipher;
    }

    @Override public void saveAssociation(UUID userId, LocalDate from, LocalDate to, String timezone,
                                          MoodSleepAssociation.Result result, Instant now) {
        String key = userId + ":sleep-mood:" + from + ":" + to;
        em.createNativeQuery("SELECT pg_advisory_xact_lock(hashtextextended(?1,0))")
                .setParameter(1, key).getSingleResult();
        boolean exists = !em.createQuery("""
                select i.id from Insight i where i.userId=:userId and i.insightType='SLEEP_MOOD_ASSOCIATION'
                    and i.periodStart=:from and i.periodEnd=:to and i.algorithmVersion=:version
                """, UUID.class).setParameter("userId", userId).setParameter("from", from)
                .setParameter("to", to).setParameter("version", VERSION).setMaxResults(1)
                .getResultList().isEmpty();
        if (exists) return;
        Insight insight = new Insight();
        insight.id = ids.next();
        insight.userId = userId;
        insight.insightType = "SLEEP_MOOD_ASSOCIATION";
        insight.periodStart = from;
        insight.periodEnd = to;
        insight.timezone = timezone;
        insight.status = "ACTIVE";
        insight.direction = result.direction();
        insight.strength = result.strength();
        insight.sampleSize = result.sampleSize();
        insight.algorithmVersion = VERSION;
        insight.metricsSnapshot = Map.of("correlation", result.correlation(),
                "averageMood", result.averageMood(), "averageSleepHours", result.averageSleepHours());
        String relationship = switch (result.direction()) {
            case "UP" -> "cùng chiều";
            case "DOWN" -> "ngược chiều";
            default -> "rất yếu";
        };
        String narrative = "Trong " + result.sampleSize()
                + " ngày có đủ dữ liệu, thời lượng ngủ và điểm tâm trạng có mối liên hệ "
                + relationship + ". Mối liên hệ này không chứng minh nguyên nhân.";
        var encrypted = cipher.encrypt("insights.narrative", userId, insight.id, narrative);
        insight.encryptedNarrative = encrypted.ciphertext();
        insight.narrativeIv = encrypted.iv();
        insight.narrativeWrappedKey = encrypted.wrappedKey();
        insight.narrativeKeyVersion = encrypted.keyVersion();
        insight.generatedBy = "STATISTICAL";
        insight.createdAt = now;
        em.persist(insight);
        for (var day : result.evidence()) {
            evidence(insight.id, day.date(), day.source(), day.sourceId(), "MOOD_SCORE",
                    day.moodScore(), now);
            evidence(insight.id, day.date(), day.source(), day.sourceId(), "SLEEP_MINUTES",
                    BigDecimal.valueOf(day.sleepMinutes()), now);
        }
    }

    @Override @Transactional
    public void replacePatterns(UUID userId, LocalDate from, LocalDate to, String timezone, String algorithmVersion,
                                List<PatternInsight> insights, Instant now) {
        em.createNativeQuery("SELECT pg_advisory_xact_lock(hashtextextended(?1,0))")
                .setParameter(1, userId + ":patterns:" + algorithmVersion).getSingleResult();
        // A type the user hid for an overlapping window stays hidden after a refresh.
        @SuppressWarnings("unchecked")
        List<String> hiddenTypes = em.createNativeQuery("""
                SELECT DISTINCT insight_type FROM insights
                WHERE user_id=?1 AND algorithm_version=?2 AND status='HIDDEN' AND period_end>=?3
                """, String.class).setParameter(1, userId).setParameter(2, algorithmVersion).setParameter(3, from)
                .getResultList();
        // A second refresh on the same day replaces that day's rows (unique user/type/period/version).
        em.createNativeQuery("""
                DELETE FROM insights WHERE user_id=?1 AND algorithm_version=?2 AND period_start=?3 AND period_end=?4
                """).setParameter(1, userId).setParameter(2, algorithmVersion).setParameter(3, from)
                .setParameter(4, to).executeUpdate();
        em.createNativeQuery("""
                UPDATE insights SET status='SUPERSEDED', superseded_at=?3
                WHERE user_id=?1 AND algorithm_version=?2 AND status='ACTIVE'
                """).setParameter(1, userId).setParameter(2, algorithmVersion).setParameter(3, now).executeUpdate();
        for (PatternInsight item : insights) {
            var finding = item.finding();
            Insight insight = new Insight();
            insight.id = ids.next();
            insight.userId = userId;
            insight.insightType = finding.type();
            insight.periodStart = from;
            insight.periodEnd = to;
            insight.timezone = timezone;
            insight.status = hiddenTypes.contains(finding.type()) ? "HIDDEN" : "ACTIVE";
            insight.direction = finding.direction();
            insight.strength = finding.strength();
            insight.sampleSize = finding.sampleSize();
            insight.algorithmVersion = algorithmVersion;
            Map<String, Object> snapshot = new LinkedHashMap<>(finding.facts());
            if (item.narrativeModel() != null) snapshot.put("narrativeModel", item.narrativeModel());
            insight.metricsSnapshot = snapshot;
            encryptNarrative(insight, userId, item.narrative());
            insight.generatedBy = item.generatedBy();
            insight.createdAt = now;
            em.persist(insight);
            for (var point : finding.evidence()) {
                InsightEvidence evidence = new InsightEvidence();
                evidence.id = ids.next();
                evidence.insightId = insight.id;
                evidence.sourceType = point.sourceType();
                evidence.sourceId = point.sourceId();
                evidence.evidenceDate = point.date();
                evidence.metricName = point.metric();
                evidence.metricValue = point.value();
                evidence.weight = BigDecimal.ONE;
                evidence.metadata = Map.of();
                evidence.createdAt = now;
                em.persist(evidence);
            }
        }
        em.flush();
    }

    @Override @Transactional
    public boolean hide(UUID userId, UUID insightId) {
        return em.createNativeQuery("""
                UPDATE insights SET status='HIDDEN' WHERE id=?1 AND user_id=?2 AND status<>'HIDDEN'
                """).setParameter(1, insightId).setParameter(2, userId).executeUpdate() > 0
                || ((Number) em.createNativeQuery("SELECT count(*) FROM insights WHERE id=?1 AND user_id=?2")
                .setParameter(1, insightId).setParameter(2, userId).getSingleResult()).longValue() > 0;
    }

    @Override @Transactional(readOnly = true)
    public List<UserZone> activeUsersAfter(UUID cursor, int limit) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT p.user_id, p.timezone FROM user_profiles p JOIN users u ON u.id=p.user_id
                WHERE u.status='ACTIVE' AND (CAST(?1 AS uuid) IS NULL OR p.user_id > CAST(?1 AS uuid))
                ORDER BY p.user_id LIMIT ?2
                """).setParameter(1, cursor == null ? null : cursor.toString()).setParameter(2, limit).getResultList();
        List<UserZone> out = new ArrayList<>(rows.size());
        for (Object[] row : rows) out.add(new UserZone((UUID) row[0], (String) row[1]));
        return out;
    }

    @Override @Transactional(readOnly = true)
    public boolean needsPatternRefresh(UUID userId, String algorithmVersion, Instant since) {
        Object result = em.createNativeQuery("""
                SELECT NOT EXISTS (SELECT 1 FROM insights WHERE user_id=?1 AND algorithm_version=?2 AND created_at>=?3)
                   AND coalesce(greatest(
                        (SELECT max(updated_at) FROM journal_entries WHERE user_id=?1),
                        (SELECT max(updated_at) FROM daily_checkins WHERE user_id=?1)), timestamptz 'epoch')
                     > coalesce((SELECT max(created_at) FROM insights WHERE user_id=?1 AND algorithm_version=?2),
                        timestamptz 'epoch')
                """).setParameter(1, userId).setParameter(2, algorithmVersion).setParameter(3, since).getSingleResult();
        return Boolean.TRUE.equals(result);
    }

    @Override @Transactional
    public void recordNarrationUsage(NarrationUsage usage, Instant now) {
        em.createNativeQuery("""
                INSERT INTO ai_usage_records(id, job_id, analysis_id, provider, model, operation, input_tokens,
                    output_tokens, estimated_cost_usd, latency_ms, request_status, created_at)
                VALUES (?1, NULL, NULL, ?2, ?3, 'INSIGHT_NARRATIVE', ?4, ?5, ?6, ?7, ?8, ?9)
                """).setParameter(1, ids.next()).setParameter(2, usage.provider()).setParameter(3, usage.model())
                .setParameter(4, usage.inputTokens()).setParameter(5, usage.outputTokens())
                .setParameter(6, usage.estimatedCostUsd())
                .setParameter(7, (int) Math.min(Integer.MAX_VALUE, Math.max(0, usage.latencyMs())))
                .setParameter(8, usage.succeeded() ? "SUCCEEDED" : "FAILED").setParameter(9, now).executeUpdate();
    }

    private void encryptNarrative(Insight insight, UUID userId, String narrative) {
        var encrypted = cipher.encrypt("insights.narrative", userId, insight.id, narrative);
        insight.encryptedNarrative = encrypted.ciphertext();
        insight.narrativeIv = encrypted.iv();
        insight.narrativeWrappedKey = encrypted.wrappedKey();
        insight.narrativeKeyVersion = encrypted.keyVersion();
    }

    private void evidence(UUID insightId, LocalDate date, String source, UUID sourceId,
                          String metric, BigDecimal value, Instant now) {
        InsightEvidence evidence = new InsightEvidence();
        evidence.id = ids.next();
        evidence.insightId = insightId;
        evidence.sourceType = "CHECKIN".equals(source) ? "CHECKIN" : "JOURNAL";
        evidence.sourceId = sourceId;
        evidence.evidenceDate = date;
        evidence.metricName = metric;
        evidence.metricValue = value;
        evidence.weight = BigDecimal.ONE;
        evidence.metadata = Map.of("source", source);
        evidence.createdAt = now;
        em.persist(evidence);
    }

    @Override public InsightPage list(UUID userId, LocalDate from, LocalDate to, String cursor, int limit) {
        Cursor decoded = decode(cursor);
        StringBuilder jpql = new StringBuilder("select i from Insight i where i.userId=:userId and i.status='ACTIVE'");
        if (from != null) jpql.append(" and i.periodStart>=:from");
        if (to != null) jpql.append(" and i.periodEnd<=:to");
        if (decoded != null) jpql.append(" and (i.periodEnd<:cursorDate or (i.periodEnd=:cursorDate and i.id<:cursorId))");
        jpql.append(" order by i.periodEnd desc, i.id desc");
        var query = em.createQuery(jpql.toString(), Insight.class).setParameter("userId", userId)
                .setMaxResults(limit + 1);
        if (from != null) query.setParameter("from", from);
        if (to != null) query.setParameter("to", to);
        if (decoded != null) query.setParameter("cursorDate", decoded.date()).setParameter("cursorId", decoded.id());
        List<Insight> rows = query.getResultList();
        boolean more = rows.size() > limit;
        List<Insight> page = more ? rows.subList(0, limit) : rows;
        List<InsightView> items = new ArrayList<>();
        for (Insight insight : page) {
            List<InsightEvidenceView> evidence = em.createQuery("""
                    select e from InsightEvidence e where e.insightId=:id
                    order by e.evidenceDate, e.metricName
                    """, InsightEvidence.class).setParameter("id", insight.id).getResultList().stream()
                    .map(e -> new InsightEvidenceView(e.sourceType, e.evidenceDate, e.metricName, e.metricValue))
                    .toList();
            String narrative = cipher.decrypt("insights.narrative", userId, insight.id,
                    new SensitiveDataCipher.Encrypted(insight.encryptedNarrative, insight.narrativeIv,
                            insight.narrativeWrappedKey, insight.narrativeKeyVersion));
            items.add(new InsightView(insight.id, insight.insightType, insight.periodStart,
                    insight.periodEnd, insight.timezone, insight.direction, insight.strength,
                    insight.sampleSize, insight.algorithmVersion, insight.generatedBy, narrative, evidence));
        }
        String next = more ? encode(page.getLast()) : null;
        return new InsightPage(items, next);
    }

    private record Cursor(LocalDate date, UUID id) {}

    private static Cursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            String value = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = value.split("\\|", 2);
            if (parts.length != 2) throw new IllegalArgumentException();
            return new Cursor(LocalDate.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (RuntimeException e) { throw new InvalidRequestException(); }
    }

    private static String encode(Insight insight) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                (insight.periodEnd + "|" + insight.id).getBytes(StandardCharsets.UTF_8));
    }
}
