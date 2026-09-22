package com.mylog.analysis.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AnalysisQueryRepository {

    private final JdbcTemplate jdbc;

    public AnalysisQueryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<JournalState> findOwnedJournal(UUID userId, UUID journalId) {
        return jdbc.query("""
                SELECT journal_version, status FROM journal_entries
                WHERE id = ? AND user_id = ? AND deleted_at IS NULL
                """, (rs, row) -> new JournalState(rs.getLong("journal_version"), rs.getString("status")),
                journalId, userId).stream().findFirst();
    }

    public Optional<AnalysisRow> findCurrentAnalysis(UUID journalId, long version) {
        return jdbc.query("""
                SELECT id, sentiment, risk_level, summary, explanation, provider, model,
                       prompt_version, analyzed_at
                FROM journal_analyses
                WHERE journal_entry_id = ? AND journal_version = ? AND is_current = TRUE
                """, (rs, row) -> new AnalysisRow(
                        rs.getObject("id", UUID.class), rs.getString("sentiment"), rs.getString("risk_level"),
                        rs.getString("summary"), rs.getString("explanation"), rs.getString("provider"),
                        rs.getString("model"), rs.getString("prompt_version"),
                        rs.getTimestamp("analyzed_at").toInstant()), journalId, version).stream().findFirst();
    }

    public List<EmotionRow> findEmotions(UUID analysisId) {
        return jdbc.query("""
                SELECT emotion_type, original_score, COALESCE(corrected_score, original_score) effective_score,
                       corrected_by_user
                FROM journal_emotions WHERE analysis_id = ? ORDER BY emotion_type
                """, (rs, row) -> new EmotionRow(
                        rs.getString("emotion_type"), rs.getBigDecimal("original_score"),
                        rs.getBigDecimal("effective_score"), rs.getBoolean("corrected_by_user")), analysisId);
    }

    public List<TopicRow> findActiveTopics(UUID journalId) {
        return jdbc.query("""
                SELECT topic.display_name, link.confidence, link.source
                FROM journal_topics link JOIN topics topic ON topic.id = link.topic_id
                WHERE link.journal_entry_id = ? AND link.is_active = TRUE
                ORDER BY topic.normalized_name
                """, (rs, row) -> new TopicRow(
                        rs.getString("display_name"), rs.getBigDecimal("confidence"), rs.getString("source")),
                journalId);
    }

    public Optional<SafetyRow> findLatestSafety(UUID userId, UUID journalId, long version) {
        return jdbc.query("""
                SELECT risk_level, action_taken FROM safety_events
                WHERE user_id = ? AND journal_entry_id = ? AND journal_version = ?
                ORDER BY created_at DESC LIMIT 1
                """, (rs, row) -> new SafetyRow(rs.getString("risk_level"), rs.getString("action_taken")),
                userId, journalId, version).stream().findFirst();
    }

    public Optional<UUID> findLatestReflectionBatch(UUID journalId, long version) {
        return jdbc.queryForList("""
                SELECT generation_batch_id FROM reflection_questions
                WHERE journal_entry_id = ? AND journal_version = ?
                ORDER BY created_at DESC LIMIT 1
                """, UUID.class, journalId, version).stream().findFirst();
    }

    public List<ReflectionQuestionRow> findReflectionQuestions(UUID batchId) {
        return jdbc.query("""
                SELECT id, position, question, created_at FROM reflection_questions
                WHERE generation_batch_id = ? ORDER BY position
                """, (rs, row) -> new ReflectionQuestionRow(
                        rs.getObject("id", UUID.class), rs.getInt("position"), rs.getString("question"),
                        rs.getTimestamp("created_at").toInstant()), batchId);
    }

    public Optional<String> findAnalysisJobStatus(UUID journalId, long version) {
        return jdbc.queryForList("""
                SELECT status FROM analysis_jobs
                WHERE journal_entry_id = ? AND journal_version = ? AND job_type = 'ANALYSIS'
                """, String.class, journalId, version).stream().findFirst();
    }

    public record JournalState(long version, String status) {}

    public record AnalysisRow(
            UUID id, String sentiment, String riskLevel, String summary, String explanation,
            String provider, String model, String promptVersion, Instant analyzedAt) {}

    public record EmotionRow(String type, BigDecimal originalScore, BigDecimal effectiveScore, boolean correctedByUser) {}

    public record TopicRow(String name, BigDecimal confidence, String source) {}

    public record SafetyRow(String riskLevel, String actionTaken) {}

    public record ReflectionQuestionRow(UUID id, int position, String question, Instant createdAt) {}
}
