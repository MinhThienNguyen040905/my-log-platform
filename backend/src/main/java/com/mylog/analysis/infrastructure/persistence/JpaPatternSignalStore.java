package com.mylog.analysis.infrastructure.persistence;

import com.mylog.analysis.application.PatternExtractor;
import com.mylog.analysis.application.PatternSignalStore;
import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.platform.id.IdGenerator;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Native SQL over V22. Only entries whose screening allowed ordinary AI processing (risk NORMAL/LOW) are ever
 * read or kept; the same condition is re-checked when saving so a late result for a changed entry is dropped.
 */
@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JpaPatternSignalStore implements PatternSignalStore {
    static final String QUOTE_FIELD = "entry_pattern_signals.quote";
    private static final String READABLE = "j.deleted_at IS NULL AND j.entry_status='SAVED' AND j.risk_level IN ('NORMAL','LOW')";

    private final EntityManager em;
    private final SensitiveDataCipher cipher;
    private final IdGenerator ids;

    JpaPatternSignalStore(EntityManager em, SensitiveDataCipher cipher, IdGenerator ids) {
        this.em = em; this.cipher = cipher; this.ids = ids;
    }

    @Override @Transactional(readOnly = true)
    public String timezone(UUID userId) {
        return (String) em.createNativeQuery("SELECT timezone FROM user_profiles WHERE user_id=?1")
                .setParameter(1, userId).getSingleResult();
    }

    @Override @Transactional(readOnly = true)
    public List<Candidate> pending(UUID userId, LocalDate from, LocalDate to, String taxonomyVersion, int limit) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT j.id, j.content_version, j.local_date FROM journal_entries j
                WHERE j.user_id=?1 AND j.local_date BETWEEN ?2 AND ?3 AND %s
                    AND NOT EXISTS (SELECT 1 FROM entry_pattern_extractions x WHERE x.journal_entry_id=j.id
                        AND x.content_version=j.content_version AND x.taxonomy_version=?4)
                ORDER BY j.local_date DESC, j.occurred_at DESC, j.id
                LIMIT ?5
                """.formatted(READABLE)).setParameter(1, userId).setParameter(2, from).setParameter(3, to)
                .setParameter(4, taxonomyVersion).setParameter(5, limit).getResultList();
        List<Candidate> out = new ArrayList<>(rows.size());
        for (Object[] row : rows)
            out.add(new Candidate((UUID) row[0], ((Number) row[1]).intValue(), (LocalDate) row[2]));
        return out;
    }

    @Override @Transactional
    public int purgeStale(UUID userId) {
        return em.createNativeQuery("""
                DELETE FROM entry_pattern_extractions x USING journal_entries j
                WHERE x.user_id=?1 AND j.id=x.journal_entry_id
                    AND (j.content_version<>x.content_version OR NOT (%s))
                """.formatted(READABLE)).setParameter(1, userId).executeUpdate();
    }

    @Override @Transactional
    public boolean save(UUID userId, Candidate candidate, String taxonomyVersion, PatternExtractor.Result result,
                        List<PatternExtractor.Signal> accepted, Instant now) {
        UUID extractionId = ids.next();
        int inserted = em.createNativeQuery("""
                INSERT INTO entry_pattern_extractions(id,user_id,journal_entry_id,content_version,taxonomy_version,
                    signal_count,extractor_provider,extractor_model,extractor_version,created_at)
                SELECT ?1,?2,j.id,j.content_version,?4,?5,?6,?7,?8,?9 FROM journal_entries j
                WHERE j.id=?3 AND j.user_id=?2 AND j.content_version=?10 AND %s
                ON CONFLICT (journal_entry_id, content_version, taxonomy_version) DO NOTHING
                """.formatted(READABLE)).setParameter(1, extractionId).setParameter(2, userId)
                .setParameter(3, candidate.entryId()).setParameter(4, taxonomyVersion)
                .setParameter(5, accepted.size()).setParameter(6, result.provider()).setParameter(7, result.model())
                .setParameter(8, result.version()).setParameter(9, Timestamp.from(now))
                .setParameter(10, candidate.contentVersion()).executeUpdate();
        if (inserted != 1) return false;
        for (PatternExtractor.Signal signal : accepted) {
            UUID signalId = ids.next();
            var quote = cipher.encrypt(QUOTE_FIELD, userId, signalId, signal.quote());
            em.createNativeQuery("""
                    INSERT INTO entry_pattern_signals(id,extraction_id,user_id,journal_entry_id,pattern_code,confidence,
                        encrypted_quote,quote_iv,quote_wrapped_key,quote_key_version,created_at)
                    VALUES (?1,?2,?3,?4,?5,?6,?7,?8,?9,?10,?11)
                    """).setParameter(1, signalId).setParameter(2, extractionId).setParameter(3, userId)
                    .setParameter(4, candidate.entryId()).setParameter(5, signal.code())
                    .setParameter(6, signal.confidence()).setParameter(7, quote.ciphertext())
                    .setParameter(8, quote.iv()).setParameter(9, quote.wrappedKey())
                    .setParameter(10, quote.keyVersion()).setParameter(11, Timestamp.from(now)).executeUpdate();
        }
        return true;
    }

    @Override @Transactional
    public void recordUsage(PatternExtractor.Result result, long latencyMs, Instant now) {
        em.createNativeQuery("""
                INSERT INTO ai_usage_records(id, job_id, analysis_id, provider, model, operation, input_tokens,
                    output_tokens, estimated_cost_usd, latency_ms, request_status, created_at)
                VALUES (?1, NULL, NULL, ?2, ?3, 'PATTERN_EXTRACTION', ?4, ?5, ?6, ?7, ?8, ?9)
                """).setParameter(1, ids.next()).setParameter(2, result.provider()).setParameter(3, result.model())
                .setParameter(4, Math.max(0, result.inputTokens())).setParameter(5, Math.max(0, result.outputTokens()))
                .setParameter(6, result.estimatedCostUsd())
                .setParameter(7, (int) Math.min(Integer.MAX_VALUE, Math.max(0, latencyMs)))
                .setParameter(8, result.succeeded() ? "SUCCEEDED" : "FAILED")
                .setParameter(9, Timestamp.from(now)).executeUpdate();
    }
}
