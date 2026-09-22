package com.mylog.insight.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

@Repository
public class InsightRepository {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public InsightRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void advanceLifecycle(UUID userId, Instant now) {
        jdbc.update("""
                UPDATE insights SET status = 'EXPIRED', updated_at = ?
                WHERE user_id = ? AND status IN ('ACTIVE', 'FADING') AND expires_at <= ?
                """, Timestamp.from(now), userId, Timestamp.from(now));
        jdbc.update("""
                UPDATE insights SET status = 'FADING', updated_at = ?
                WHERE user_id = ? AND status = 'ACTIVE' AND updated_at < ?
                """, Timestamp.from(now), userId, Timestamp.from(now.minusSeconds(14L * 24 * 60 * 60)));
    }

    public UUID upsertInsight(
            UUID userId, String fingerprint, String title, String description, String confidence,
            LocalDate from, LocalDate to, Instant now) {
        Optional<UUID> existing = jdbc.queryForList("""
                SELECT id FROM insights
                WHERE user_id = ? AND fingerprint = ? AND status IN ('ACTIVE', 'FADING')
                ORDER BY updated_at DESC LIMIT 1
                """, UUID.class, userId, fingerprint).stream().findFirst();
        if (existing.isPresent()) {
            UUID id = existing.get();
            jdbc.update("""
                    UPDATE insights SET title = ?, description = ?, confidence = ?, status = 'ACTIVE',
                        period_start = ?, period_end = ?, updated_at = ?, expires_at = ?, version = version + 1
                    WHERE id = ?
                    """, title, description, confidence, from, to, Timestamp.from(now),
                    Timestamp.from(now.plusSeconds(30L * 24 * 60 * 60)), id);
            return id;
        }
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO insights (
                    id, user_id, type, fingerprint, title, description, confidence, status,
                    period_start, period_end, created_at, updated_at, expires_at, version)
                VALUES (?, ?, 'TOPIC_MOOD_ASSOCIATION', ?, ?, ?, ?, 'ACTIVE', ?, ?, ?, ?, ?, 0)
                """, id, userId, fingerprint, title, description, confidence, from, to,
                Timestamp.from(now), Timestamp.from(now),
                Timestamp.from(now.plusSeconds(30L * 24 * 60 * 60)));
        return id;
    }

    public void replaceEvidence(
            UUID insightId, int sampleSize, int matchingCount, BigDecimal moodDelta,
            Map<String, Object> details, Instant now) {
        jdbc.update("DELETE FROM insight_evidence WHERE insight_id = ?", insightId);
        jdbc.update("""
                INSERT INTO insight_evidence (
                    id, insight_id, evidence_type, sample_size, matching_count, metric,
                    numeric_value, unit, evidence_json, calculation_version, created_at)
                VALUES (?, ?, 'TOPIC_MOOD_ASSOCIATION', ?, ?, 'MOOD_DELTA', ?, 'SCORE', ?::jsonb,
                        'topic_mood_v1', ?)
                """, UUID.randomUUID(), insightId, sampleSize, matchingCount, moodDelta,
                objectMapper.writeValueAsString(details), Timestamp.from(now));
    }

    public void ensureSuggestedAction(UUID userId, UUID insightId, String description, Instant now) {
        jdbc.update("""
                INSERT INTO suggested_actions (id, user_id, insight_id, description, status, created_at)
                SELECT ?, ?, ?, ?, 'PENDING', ?
                WHERE NOT EXISTS (
                    SELECT 1 FROM suggested_actions WHERE insight_id = ? AND status = 'PENDING'
                )
                """, UUID.randomUUID(), userId, insightId, description, Timestamp.from(now), insightId);
    }

}
