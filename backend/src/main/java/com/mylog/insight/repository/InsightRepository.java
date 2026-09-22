package com.mylog.insight.repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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

    public UserLocale findUserLocale(UUID userId) {
        return jdbc.query("""
                SELECT COALESCE(preference.timezone, 'UTC') AS timezone,
                       COALESCE(preference.language, 'vi') AS language
                FROM users user_account
                LEFT JOIN user_preferences preference ON preference.user_id = user_account.id
                WHERE user_account.id = ?
                """, (rs, row) -> new UserLocale(rs.getString("timezone"), rs.getString("language")), userId)
                .stream().findFirst().orElse(new UserLocale("UTC", "vi"));
    }

    public List<TopicMoodCandidate> topicMoodCandidates(
            UUID userId, LocalDate from, LocalDate to, String timezone) {
        return jdbc.query("""
                WITH base AS (
                    SELECT journal.id, journal.mood_score, journal.journal_version,
                           CAST(journal.occurred_at AT TIME ZONE ? AS date) AS local_date
                    FROM journal_entries journal
                    WHERE journal.user_id = ? AND journal.deleted_at IS NULL
                      AND CAST(journal.occurred_at AT TIME ZONE ? AS date) BETWEEN ? AND ?
                ), totals AS (
                    SELECT COUNT(*)::int AS journal_count, AVG(mood_score) AS overall_mood FROM base
                )
                SELECT topic.normalized_name, topic.display_name,
                       COUNT(DISTINCT base.id)::int AS matching_count,
                       COUNT(DISTINCT base.local_date)::int AS distinct_days,
                       totals.journal_count,
                       ROUND(AVG(base.mood_score), 2) AS topic_mood,
                       ROUND(totals.overall_mood, 2) AS overall_mood,
                       EXISTS (
                           SELECT 1 FROM base risk_base
                           JOIN journal_analyses risk_analysis ON risk_analysis.journal_entry_id = risk_base.id
                               AND risk_analysis.journal_version = risk_base.journal_version
                               AND risk_analysis.is_current = TRUE
                           WHERE risk_analysis.risk_level IN ('HIGH', 'CRITICAL')
                       ) AS safety_blocked
                FROM base
                JOIN journal_topics link ON link.journal_entry_id = base.id AND link.is_active = TRUE
                JOIN topics topic ON topic.id = link.topic_id
                CROSS JOIN totals
                GROUP BY topic.id, topic.normalized_name, topic.display_name,
                         totals.journal_count, totals.overall_mood
                HAVING COUNT(DISTINCT base.local_date) >= 3
                ORDER BY matching_count DESC, topic.normalized_name
                """, (rs, row) -> new TopicMoodCandidate(
                        rs.getString("normalized_name"), rs.getString("display_name"),
                        rs.getInt("matching_count"), rs.getInt("distinct_days"), rs.getInt("journal_count"),
                        rs.getBigDecimal("topic_mood"), rs.getBigDecimal("overall_mood"),
                        rs.getBoolean("safety_blocked")),
                timezone, userId, timezone, from, to);
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

    public List<InsightRow> findVisible(UUID userId) {
        return jdbc.query("""
                SELECT id, type, title, description, confidence, status, period_start, period_end,
                       created_at, updated_at
                FROM insights WHERE user_id = ? AND status IN ('ACTIVE', 'FADING')
                ORDER BY period_end DESC, confidence DESC, updated_at DESC
                """, (rs, row) -> insightRow(rs), userId);
    }

    public Optional<InsightRow> findOwned(UUID userId, UUID insightId) {
        return jdbc.query("""
                SELECT id, type, title, description, confidence, status, period_start, period_end,
                       created_at, updated_at
                FROM insights WHERE id = ? AND user_id = ?
                """, (rs, row) -> insightRow(rs), insightId, userId).stream().findFirst();
    }

    public List<EvidenceRow> findEvidence(UUID insightId) {
        return jdbc.query("""
                SELECT id, evidence_type, sample_size, matching_count, metric, numeric_value,
                       unit, evidence_json::text AS evidence_json, calculation_version
                FROM insight_evidence WHERE insight_id = ? ORDER BY created_at
                """, (rs, row) -> new EvidenceRow(
                        rs.getObject("id", UUID.class), rs.getString("evidence_type"), rs.getInt("sample_size"),
                        (Integer) rs.getObject("matching_count"), rs.getString("metric"),
                        rs.getBigDecimal("numeric_value"), rs.getString("unit"), rs.getString("evidence_json"),
                        rs.getString("calculation_version")), insightId);
    }

    public List<ActionRow> findActions(UUID insightId) {
        return jdbc.query("""
                SELECT id, description, status, created_at, responded_at
                FROM suggested_actions WHERE insight_id = ?
                ORDER BY created_at DESC LIMIT 1
                """, (rs, row) -> new ActionRow(
                        rs.getObject("id", UUID.class), rs.getString("description"), rs.getString("status"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("responded_at") == null ? null : rs.getTimestamp("responded_at").toInstant()),
                insightId);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> parseDetails(String json) {
        return objectMapper.readValue(json, Map.class);
    }

    private InsightRow insightRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new InsightRow(
                rs.getObject("id", UUID.class), rs.getString("type"), rs.getString("title"),
                rs.getString("description"), rs.getString("confidence"), rs.getString("status"),
                rs.getObject("period_start", LocalDate.class), rs.getObject("period_end", LocalDate.class),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    public record UserLocale(String timezone, String language) {}

    public record TopicMoodCandidate(
            String normalizedName, String displayName, int matchingCount, int distinctDays,
            int journalCount, BigDecimal topicMood, BigDecimal overallMood, boolean safetyBlocked) {
        public BigDecimal matchingRatio() {
            return journalCount == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(matchingCount)
                    .divide(BigDecimal.valueOf(journalCount), 4, RoundingMode.HALF_UP);
        }

        public BigDecimal moodDelta() {
            return topicMood.subtract(overallMood).setScale(2, RoundingMode.HALF_UP);
        }
    }

    public record InsightRow(
            UUID id, String type, String title, String description, String confidence, String status,
            LocalDate periodStart, LocalDate periodEnd, Instant createdAt, Instant updatedAt) {}

    public record EvidenceRow(
            UUID id, String type, int sampleSize, Integer matchingCount, String metric,
            BigDecimal numericValue, String unit, String detailsJson, String calculationVersion) {}

    public record ActionRow(UUID id, String description, String status, Instant createdAt, Instant respondedAt) {}
}
