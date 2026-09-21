package com.mylog.analysis.application;

import com.mylog.analysis.infrastructure.persistence.AnalysisJobRepository;
import com.mylog.shared.messaging.MessagingTopology;
import com.mylog.shared.outbox.OutboxWriter;
import java.sql.Timestamp;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AnalysisResultStore {

    private final JdbcTemplate jdbc;
    private final OutboxWriter outboxWriter;
    private final Clock clock;

    public AnalysisResultStore(JdbcTemplate jdbc, OutboxWriter outboxWriter, Clock clock) {
        this.jdbc = jdbc;
        this.outboxWriter = outboxWriter;
        this.clock = clock;
    }

    @Transactional
    public boolean saveAnalysis(
            AnalysisJob job,
            AiAnalysisOutput output,
            DeterministicSafetyPolicy.SafetyDecision safety,
            boolean providerCalled) {
        Instant now = clock.instant();
        if (!isCurrent(job)) {
            markObsolete(job.id(), now);
            return false;
        }
        jdbc.update("UPDATE journal_analyses SET is_current = FALSE WHERE journal_entry_id = ? AND is_current = TRUE", job.journalId());
        UUID analysisId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO journal_analyses (
                    id, journal_entry_id, journal_version, sentiment, risk_level, summary, explanation,
                    provider, model, prompt_version, schema_version, input_token_count, output_token_count,
                    latency_ms, is_current, analyzed_at, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, ?, ?)
                """, analysisId, job.journalId(), job.journalVersion(), output.sentiment(),
                effectiveRisk(safety.riskLevel(), output.riskLevel()), output.summary(), output.explanation(),
                output.provider(), output.model(), output.promptVersion(), output.schemaVersion(),
                output.inputTokens(), output.outputTokens(), output.latencyMs(), Timestamp.from(now), Timestamp.from(now));

        for (AiAnalysisOutput.Emotion emotion : output.emotions()) {
            jdbc.update("""
                    INSERT INTO journal_emotions (
                        id, analysis_id, emotion_type, original_score, corrected_by_user, created_at)
                    VALUES (?, ?, ?, ?, FALSE, ?)
                    """, UUID.randomUUID(), analysisId, emotion.type(), emotion.score(), Timestamp.from(now));
        }
        jdbc.update("""
                UPDATE journal_topics SET is_active = FALSE, updated_at = ?
                WHERE journal_entry_id = ? AND source = 'AI'
                """, Timestamp.from(now), job.journalId());
        for (AiAnalysisOutput.Topic topic : output.topics()) {
            UUID topicId = upsertTopic(topic.name(), now);
            jdbc.update("""
                    INSERT INTO journal_topics (
                        journal_entry_id, topic_id, source, confidence, is_active, created_at, updated_at)
                    VALUES (?, ?, 'AI', ?, TRUE, ?, ?)
                    ON CONFLICT (journal_entry_id, topic_id) DO UPDATE
                    SET confidence = EXCLUDED.confidence, is_active = TRUE, updated_at = EXCLUDED.updated_at
                    """, job.journalId(), topicId, topic.confidence(), Timestamp.from(now), Timestamp.from(now));
        }
        saveReflections(job, output, now);
        String risk = effectiveRisk(safety.riskLevel(), output.riskLevel());
        if (!"NORMAL".equals(risk)) {
            String actionTaken = safety.actionTaken();
            if ("NONE".equals(actionTaken)) {
                actionTaken = "CRITICAL".equals(risk) ? "SHOW_CRISIS_SUPPORT" : "SHOW_URGENT_SUPPORT";
            }
            String detectionSource = providerCalled
                    ? ("NORMAL".equals(safety.riskLevel()) ? "AI" : "COMBINED")
                    : "RULE";
            jdbc.update("""
                    INSERT INTO safety_events (
                        id, user_id, journal_entry_id, journal_version, risk_level, detection_source,
                        action_taken, provider, model, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, UUID.randomUUID(), job.userId(), job.journalId(), job.journalVersion(), risk,
                    detectionSource, actionTaken,
                    providerCalled ? output.provider() : null, providerCalled ? output.model() : null,
                    Timestamp.from(now));
        }
        if (providerCalled) {
            saveUsage(job, output, true, null, now);
        }
        jdbc.update("""
                UPDATE analysis_jobs SET status = 'COMPLETED', completed_at = ?, updated_at = ? WHERE id = ?
                """, Timestamp.from(now), Timestamp.from(now), job.id());
        jdbc.update("""
                UPDATE journal_entries SET status = 'ANALYZED', updated_at = ?
                WHERE id = ? AND user_id = ? AND journal_version = ? AND deleted_at IS NULL
                """, Timestamp.from(now), job.journalId(), job.userId(), job.journalVersion());
        outboxWriter.append("JOURNAL", job.journalId(), MessagingTopology.JOURNAL_ANALYSIS_COMPLETED, 1,
                eventPayload(job));
        return true;
    }

    @Transactional
    public boolean saveReflectionsOnly(AnalysisJob job, AiAnalysisOutput output) {
        Instant now = clock.instant();
        if (!isCurrent(job)) {
            markObsolete(job.id(), now);
            return false;
        }
        saveReflections(job, output, now);
        saveUsage(job, output, true, null, now);
        jdbc.update("UPDATE analysis_jobs SET status = 'COMPLETED', completed_at = ?, updated_at = ? WHERE id = ?",
                Timestamp.from(now), Timestamp.from(now), job.id());
        return true;
    }

    @Transactional
    public void recordFailure(AnalysisJob job, String code, long latencyMs) {
        Instant now = clock.instant();
        jdbc.update("""
                INSERT INTO ai_usage_records (
                    id, user_id, journal_entry_id, job_id, provider, model, operation,
                    latency_ms, success, error_code, created_at)
                VALUES (?, ?, ?, ?, 'configured', 'configured', ?, ?, FALSE, ?, ?)
                """, UUID.randomUUID(), job.userId(), job.journalId(), job.id(), job.jobType(),
                Math.max(0, latencyMs), code, Timestamp.from(now));
    }

    private void saveReflections(AnalysisJob job, AiAnalysisOutput output, Instant now) {
        if (output.reflections().isEmpty()) {
            return;
        }
        UUID batchId = UUID.randomUUID();
        short position = 1;
        for (String question : output.reflections()) {
            jdbc.update("""
                    INSERT INTO reflection_questions (
                        id, user_id, journal_entry_id, journal_version, generation_batch_id,
                        position, question, provider, model, prompt_version, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, UUID.randomUUID(), job.userId(), job.journalId(), job.journalVersion(), batchId,
                    position++, question, output.provider(), output.model(), output.promptVersion(), Timestamp.from(now));
        }
    }

    private void saveUsage(AnalysisJob job, AiAnalysisOutput output, boolean success, String error, Instant now) {
        jdbc.update("""
                INSERT INTO ai_usage_records (
                    id, user_id, journal_entry_id, job_id, provider, model, operation,
                    input_token_count, output_token_count, latency_ms, success, error_code, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), job.userId(), job.journalId(), job.id(), output.provider(), output.model(),
                job.jobType(), output.inputTokens(), output.outputTokens(), output.latencyMs(), success, error,
                Timestamp.from(now));
    }

    private UUID upsertTopic(String displayName, Instant now) {
        String normalized = Normalizer.normalize(displayName.trim(), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        return jdbc.queryForObject("""
                INSERT INTO topics (id, normalized_name, display_name, created_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (normalized_name) DO UPDATE SET display_name = topics.display_name
                RETURNING id
                """, UUID.class, UUID.randomUUID(), normalized, displayName.trim(), Timestamp.from(now));
    }

    private boolean isCurrent(AnalysisJob job) {
        Boolean current = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM journal_entries
                    WHERE id = ? AND user_id = ? AND journal_version = ? AND deleted_at IS NULL)
                """, Boolean.class, job.journalId(), job.userId(), job.journalVersion());
        return Boolean.TRUE.equals(current);
    }

    private void markObsolete(UUID jobId, Instant now) {
        jdbc.update("UPDATE analysis_jobs SET status = 'OBSOLETE', completed_at = ?, updated_at = ? WHERE id = ?",
                Timestamp.from(now), Timestamp.from(now), jobId);
    }

    private String effectiveRisk(String ruleRisk, String aiRisk) {
        return rank(ruleRisk) >= rank(aiRisk) ? ruleRisk : aiRisk;
    }

    private int rank(String value) {
        return switch (value) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 3;
            case "MODERATE" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }

    private Map<String, Object> eventPayload(AnalysisJob job) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("journalId", job.journalId().toString());
        payload.put("userId", job.userId().toString());
        payload.put("journalVersion", job.journalVersion());
        return payload;
    }
}
