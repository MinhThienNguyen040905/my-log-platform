package com.mylog.insight.repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class InsightCandidateRepository {

    private final JdbcTemplate jdbc;

    public InsightCandidateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
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
}
