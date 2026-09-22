package com.mylog.statistics.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StatisticsRepository {

    private final JdbcTemplate jdbc;

    public StatisticsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public String findTimezone(UUID userId) {
        return jdbc.queryForList(
                "SELECT timezone FROM user_preferences WHERE user_id = ?",
                String.class, userId).stream().findFirst().orElse("UTC");
    }

    public List<DailyMoodRow> dailyMood(UUID userId, LocalDate from, LocalDate to, String timezone) {
        return jdbc.query("""
                SELECT CAST(occurred_at AT TIME ZONE ? AS date) AS local_date,
                       COUNT(*) AS journal_count,
                       ROUND(AVG(mood_score), 2) AS mood_average,
                       ROUND(AVG(stress_score), 2) AS stress_average,
                       ROUND(AVG(energy_score), 2) AS energy_average
                FROM journal_entries
                WHERE user_id = ? AND deleted_at IS NULL
                  AND CAST(occurred_at AT TIME ZONE ? AS date) BETWEEN ? AND ?
                GROUP BY local_date ORDER BY local_date
                """, (rs, row) -> new DailyMoodRow(
                        rs.getObject("local_date", LocalDate.class), rs.getInt("journal_count"),
                        rs.getBigDecimal("mood_average"), rs.getBigDecimal("stress_average"),
                        rs.getBigDecimal("energy_average")),
                timezone, userId, timezone, from, to);
    }

    public List<DayOfWeekRow> dayOfWeekMood(UUID userId, LocalDate from, LocalDate to, String timezone) {
        return jdbc.query("""
                SELECT EXTRACT(ISODOW FROM occurred_at AT TIME ZONE ?)::int AS day_of_week,
                       COUNT(*) AS sample_size, ROUND(AVG(mood_score), 2) AS mood_average
                FROM journal_entries
                WHERE user_id = ? AND deleted_at IS NULL
                  AND CAST(occurred_at AT TIME ZONE ? AS date) BETWEEN ? AND ?
                GROUP BY day_of_week ORDER BY day_of_week
                """, (rs, row) -> new DayOfWeekRow(
                        rs.getInt("day_of_week"), rs.getInt("sample_size"), rs.getBigDecimal("mood_average")),
                timezone, userId, timezone, from, to);
    }

    public List<EmotionRow> emotions(UUID userId, LocalDate from, LocalDate to, String timezone) {
        return jdbc.query("""
                SELECT emotion.emotion_type, COUNT(DISTINCT journal.id) AS sample_size,
                       ROUND(AVG(COALESCE(emotion.corrected_score, emotion.original_score)), 4) AS average_score
                FROM journal_entries journal
                JOIN journal_analyses analysis ON analysis.journal_entry_id = journal.id
                    AND analysis.journal_version = journal.journal_version AND analysis.is_current = TRUE
                JOIN journal_emotions emotion ON emotion.analysis_id = analysis.id
                WHERE journal.user_id = ? AND journal.deleted_at IS NULL
                  AND CAST(journal.occurred_at AT TIME ZONE ? AS date) BETWEEN ? AND ?
                GROUP BY emotion.emotion_type ORDER BY average_score DESC, emotion.emotion_type
                """, (rs, row) -> new EmotionRow(
                        rs.getString("emotion_type"), rs.getInt("sample_size"), rs.getBigDecimal("average_score")),
                userId, timezone, from, to);
    }

    public int journalCount(UUID userId, LocalDate from, LocalDate to, String timezone) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM journal_entries
                WHERE user_id = ? AND deleted_at IS NULL
                  AND CAST(occurred_at AT TIME ZONE ? AS date) BETWEEN ? AND ?
                """, Integer.class, userId, timezone, from, to);
        return count == null ? 0 : count;
    }

    public List<TopicRow> topics(UUID userId, LocalDate from, LocalDate to, String timezone) {
        return jdbc.query("""
                SELECT topic.display_name, COUNT(DISTINCT journal.id) AS journal_count
                FROM journal_entries journal
                JOIN journal_topics link ON link.journal_entry_id = journal.id AND link.is_active = TRUE
                JOIN topics topic ON topic.id = link.topic_id
                WHERE journal.user_id = ? AND journal.deleted_at IS NULL
                  AND CAST(journal.occurred_at AT TIME ZONE ? AS date) BETWEEN ? AND ?
                GROUP BY topic.id, topic.display_name
                ORDER BY journal_count DESC, topic.display_name LIMIT 20
                """, (rs, row) -> new TopicRow(rs.getString("display_name"), rs.getInt("journal_count")),
                userId, timezone, from, to);
    }

    public record DailyMoodRow(
            LocalDate date, int journalCount, BigDecimal moodAverage,
            BigDecimal stressAverage, BigDecimal energyAverage) {}

    public record DayOfWeekRow(int dayOfWeek, int sampleSize, BigDecimal moodAverage) {}

    public record EmotionRow(String type, int sampleSize, BigDecimal averageScore) {}

    public record TopicRow(String name, int journalCount) {}
}
