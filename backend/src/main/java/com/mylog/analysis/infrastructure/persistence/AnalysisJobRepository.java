package com.mylog.analysis.infrastructure.persistence;

import com.mylog.analysis.application.AnalysisJob;
import com.mylog.analysis.configuration.AiProperties;
import com.mylog.shared.messaging.MessagingTopology;
import com.mylog.shared.outbox.OutboxWriter;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class AnalysisJobRepository {

    private final JdbcTemplate jdbc;
    private final AiProperties properties;
    private final Clock clock;
    private final OutboxWriter outboxWriter;

    public AnalysisJobRepository(
            JdbcTemplate jdbc, AiProperties properties, Clock clock, OutboxWriter outboxWriter) {
        this.jdbc = jdbc;
        this.properties = properties;
        this.clock = clock;
        this.outboxWriter = outboxWriter;
    }

    @Transactional
    public void enqueue(UUID userId, UUID journalId, long journalVersion, String jobType) {
        Instant now = clock.instant();
        jdbc.update("""
                INSERT INTO analysis_jobs (
                    id, user_id, journal_entry_id, journal_version, job_type, status,
                    attempt_count, max_attempts, next_attempt_at, created_at, updated_at)
                SELECT ?, ?, id, ?, ?, 'PENDING', 0, ?, ?, ?, ?
                FROM journal_entries
                WHERE id = ? AND user_id = ? AND journal_version = ? AND deleted_at IS NULL
                ON CONFLICT (journal_entry_id, journal_version, job_type) DO NOTHING
                """,
                UUID.randomUUID(), userId, journalVersion, jobType, properties.maxAttempts(),
                Timestamp.from(now), Timestamp.from(now), Timestamp.from(now),
                journalId, userId, journalVersion);
        if ("ANALYSIS".equals(jobType)) {
            jdbc.update("""
                    UPDATE journal_entries SET status = 'ANALYZING', updated_at = ?
                    WHERE id = ? AND user_id = ? AND journal_version = ? AND deleted_at IS NULL
                      AND status IN ('SAVED', 'ANALYSIS_FAILED', 'ANALYSIS_OUTDATED')
                    """, Timestamp.from(now), journalId, userId, journalVersion);
        }
    }

    @Transactional
    public boolean retry(UUID userId, UUID journalId, long journalVersion, String jobType) {
        Instant now = clock.instant();
        int changed = jdbc.update("""
                UPDATE analysis_jobs
                SET status = 'PENDING', attempt_count = 0, next_attempt_at = ?, started_at = NULL,
                    completed_at = NULL, last_error_code = NULL, last_error_at = NULL, updated_at = ?
                WHERE user_id = ? AND journal_entry_id = ? AND journal_version = ? AND job_type = ?
                  AND status IN ('FAILED', 'COMPLETED', 'OBSOLETE', 'CANCELLED')
                """, Timestamp.from(now), Timestamp.from(now), userId, journalId, journalVersion, jobType);
        if (changed == 0) {
            enqueue(userId, journalId, journalVersion, jobType);
            changed = 1;
        }
        if ("ANALYSIS".equals(jobType)) {
            jdbc.update("""
                    UPDATE journal_entries SET status = 'ANALYZING', updated_at = ?
                    WHERE id = ? AND user_id = ? AND journal_version = ? AND deleted_at IS NULL
                    """, Timestamp.from(now), journalId, userId, journalVersion);
        }
        return changed > 0;
    }

    @Transactional
    public List<AnalysisJob> claimBatch() {
        Instant now = clock.instant();
        Instant staleBefore = now.minus(properties.claimLease());
        return jdbc.query("""
                WITH candidates AS (
                    SELECT id FROM analysis_jobs
                    WHERE ((status IN ('PENDING', 'RETRY_WAIT') AND (next_attempt_at IS NULL OR next_attempt_at <= ?))
                           OR (status = 'PROCESSING' AND started_at < ?))
                      AND job_type IN ('ANALYSIS', 'REFLECTION')
                    ORDER BY created_at
                    FOR UPDATE SKIP LOCKED
                    LIMIT ?
                )
                UPDATE analysis_jobs job
                SET status = 'PROCESSING', attempt_count = job.attempt_count + 1,
                    started_at = ?, updated_at = ?
                FROM candidates
                WHERE job.id = candidates.id
                RETURNING job.id, job.user_id, job.journal_entry_id, job.journal_version,
                          job.job_type, job.attempt_count, job.max_attempts, job.started_at
                """,
                (rs, row) -> new AnalysisJob(
                        rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class),
                        rs.getObject("journal_entry_id", UUID.class), rs.getLong("journal_version"),
                        rs.getString("job_type"), rs.getInt("attempt_count"), rs.getInt("max_attempts"),
                        rs.getTimestamp("started_at").toInstant()),
                Timestamp.from(now), Timestamp.from(staleBefore), properties.batchSize(),
                Timestamp.from(now), Timestamp.from(now));
    }

    public Optional<JournalInput> loadCurrentInput(AnalysisJob job) {
        return jdbc.query("""
                SELECT content_text, mood_score, stress_score, energy_score
                FROM journal_entries
                WHERE id = ? AND user_id = ? AND journal_version = ? AND deleted_at IS NULL
                """, rs -> rs.next() ? Optional.of(new JournalInput(
                        rs.getString("content_text"), rs.getInt("mood_score"),
                        (Integer) rs.getObject("stress_score"), (Integer) rs.getObject("energy_score"))) : Optional.empty(),
                job.journalId(), job.userId(), job.journalVersion());
    }

    public void markObsolete(UUID jobId) {
        finish(jobId, "OBSOLETE", null);
    }

    public void markCompleted(UUID jobId) {
        finish(jobId, "COMPLETED", null);
    }

    @Transactional
    public void markFailure(AnalysisJob job, String errorCode, boolean retryable) {
        Instant now = clock.instant();
        boolean willRetry = retryable && job.attemptCount() < job.maxAttempts();
        Instant next = willRetry ? now.plus(backoff(job.attemptCount())) : null;
        jdbc.update("""
                UPDATE analysis_jobs
                SET status = ?, next_attempt_at = ?, last_error_code = ?, last_error_at = ?,
                    completed_at = ?, updated_at = ?
                WHERE id = ?
                """, willRetry ? "RETRY_WAIT" : "FAILED",
                next == null ? null : Timestamp.from(next), errorCode, Timestamp.from(now),
                willRetry ? null : Timestamp.from(now), Timestamp.from(now), job.id());
        if (!willRetry && "ANALYSIS".equals(job.jobType())) {
            jdbc.update("""
                    UPDATE journal_entries SET status = 'ANALYSIS_FAILED', updated_at = ?
                    WHERE id = ? AND user_id = ? AND journal_version = ? AND deleted_at IS NULL
                    """, Timestamp.from(now), job.journalId(), job.userId(), job.journalVersion());
            outboxWriter.append(
                    "JOURNAL", job.journalId(), MessagingTopology.JOURNAL_ANALYSIS_FAILED, 1,
                    Map.of(
                            "journalId", job.journalId().toString(),
                            "userId", job.userId().toString(),
                            "journalVersion", job.journalVersion(),
                            "errorCode", errorCode));
        }
    }

    private void finish(UUID jobId, String status, String errorCode) {
        Instant now = clock.instant();
        jdbc.update("""
                UPDATE analysis_jobs SET status = ?, completed_at = ?, updated_at = ?, last_error_code = ?
                WHERE id = ?
                """, status, Timestamp.from(now), Timestamp.from(now), errorCode, jobId);
    }

    private java.time.Duration backoff(int attempt) {
        long multiplier = 1L << Math.min(Math.max(attempt - 1, 0), 20);
        java.time.Duration candidate = properties.retryBaseDelay().multipliedBy(multiplier);
        return candidate.compareTo(properties.retryMaxDelay()) > 0 ? properties.retryMaxDelay() : candidate;
    }

    public record JournalInput(String content, int moodScore, Integer stressScore, Integer energyScore) {}
}
