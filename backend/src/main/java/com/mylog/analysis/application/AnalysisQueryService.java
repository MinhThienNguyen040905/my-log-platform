package com.mylog.analysis.application;

import com.mylog.analysis.api.AnalysisResponse;
import com.mylog.analysis.api.ReflectionResponse;
import com.mylog.shared.api.ApiErrorCodes;
import com.mylog.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisQueryService {

    private final JdbcTemplate jdbc;

    public AnalysisQueryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public AnalysisResponse get(UUID userId, UUID journalId) {
        JournalState journal = ownedJournal(userId, journalId);
        List<AnalysisRow> rows = jdbc.query("""
                SELECT id, sentiment, risk_level, summary, explanation, provider, model,
                       prompt_version, analyzed_at
                FROM journal_analyses
                WHERE journal_entry_id = ? AND journal_version = ? AND is_current = TRUE
                """, (rs, row) -> new AnalysisRow(
                        rs.getObject("id", UUID.class), rs.getString("sentiment"), rs.getString("risk_level"),
                        rs.getString("summary"), rs.getString("explanation"), rs.getString("provider"),
                        rs.getString("model"), rs.getString("prompt_version"),
                        rs.getTimestamp("analyzed_at").toInstant()), journalId, journal.version());
        AnalysisResponse.Result result = rows.isEmpty() ? null : result(journalId, rows.getFirst());
        AnalysisResponse.Safety safety = latestSafety(userId, journalId, journal.version(), result);
        return new AnalysisResponse(journalId, journal.version(), apiStatus(journal.status(), journalId, journal.version()), result, safety);
    }

    @Transactional(readOnly = true)
    public ReflectionResponse reflections(UUID userId, UUID journalId) {
        JournalState journal = ownedJournal(userId, journalId);
        List<UUID> batches = jdbc.queryForList("""
                SELECT generation_batch_id FROM reflection_questions
                WHERE journal_entry_id = ? AND journal_version = ?
                ORDER BY created_at DESC LIMIT 1
                """, UUID.class, journalId, journal.version());
        if (batches.isEmpty()) {
            return new ReflectionResponse(journalId, journal.version(), null, List.of());
        }
        UUID batch = batches.getFirst();
        List<ReflectionResponse.Question> questions = jdbc.query("""
                SELECT id, position, question, created_at FROM reflection_questions
                WHERE generation_batch_id = ? ORDER BY position
                """, (rs, row) -> new ReflectionResponse.Question(
                        rs.getObject("id", UUID.class), rs.getInt("position"), rs.getString("question"),
                        rs.getTimestamp("created_at").toInstant()), batch);
        return new ReflectionResponse(journalId, journal.version(), batch, questions);
    }

    public JournalState ownedJournal(UUID userId, UUID journalId) {
        List<JournalState> rows = jdbc.query("""
                SELECT journal_version, status FROM journal_entries
                WHERE id = ? AND user_id = ? AND deleted_at IS NULL
                """, (rs, row) -> new JournalState(rs.getLong("journal_version"), rs.getString("status")), journalId, userId);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException(ApiErrorCodes.JOURNAL_NOT_FOUND, "Journal entry does not exist");
        }
        return rows.getFirst();
    }

    private AnalysisResponse.Result result(UUID journalId, AnalysisRow row) {
        List<AnalysisResponse.Emotion> emotions = jdbc.query("""
                SELECT emotion_type, original_score, COALESCE(corrected_score, original_score) effective_score,
                       corrected_by_user
                FROM journal_emotions WHERE analysis_id = ? ORDER BY emotion_type
                """, (rs, index) -> new AnalysisResponse.Emotion(
                        rs.getString("emotion_type"), rs.getBigDecimal("original_score"),
                        rs.getBigDecimal("effective_score"), rs.getBoolean("corrected_by_user")), row.id());
        List<AnalysisResponse.Topic> topics = jdbc.query("""
                SELECT topic.display_name, link.confidence, link.source
                FROM journal_topics link JOIN topics topic ON topic.id = link.topic_id
                WHERE link.journal_entry_id = ? AND link.is_active = TRUE
                ORDER BY topic.normalized_name
                """, (rs, index) -> new AnalysisResponse.Topic(
                        rs.getString("display_name"), rs.getBigDecimal("confidence"), rs.getString("source")), journalId);
        return new AnalysisResponse.Result(
                row.id(), row.sentiment(), row.riskLevel(), row.summary(), row.explanation(), emotions, topics,
                row.provider(), row.model(), row.promptVersion(), row.analyzedAt());
    }

    private AnalysisResponse.Safety latestSafety(
            UUID userId, UUID journalId, long version, AnalysisResponse.Result result) {
        List<AnalysisResponse.Safety> rows = jdbc.query("""
                SELECT risk_level, action_taken FROM safety_events
                WHERE user_id = ? AND journal_entry_id = ? AND journal_version = ?
                ORDER BY created_at DESC LIMIT 1
                """, (rs, row) -> new AnalysisResponse.Safety(
                        rs.getString("risk_level"), true, rs.getString("action_taken")), userId, journalId, version);
        if (!rows.isEmpty()) {
            return rows.getFirst();
        }
        String risk = result == null ? "NORMAL" : result.riskLevel();
        return new AnalysisResponse.Safety(risk, "HIGH".equals(risk) || "CRITICAL".equals(risk), "NONE");
    }

    private String apiStatus(String journalStatus, UUID journalId, long version) {
        if ("ANALYZED".equals(journalStatus)) return "COMPLETED";
        if ("ANALYSIS_FAILED".equals(journalStatus)) return "FAILED";
        if ("ANALYSIS_OUTDATED".equals(journalStatus)) return "OUTDATED";
        List<String> jobs = jdbc.queryForList("""
                SELECT status FROM analysis_jobs
                WHERE journal_entry_id = ? AND journal_version = ? AND job_type = 'ANALYSIS'
                """, String.class, journalId, version);
        return !jobs.isEmpty() && "PROCESSING".equals(jobs.getFirst()) ? "PROCESSING" : "PENDING";
    }

    public record JournalState(long version, String status) {}
    private record AnalysisRow(
            UUID id, String sentiment, String riskLevel, String summary, String explanation,
            String provider, String model, String promptVersion, java.time.Instant analyzedAt) {}
}
