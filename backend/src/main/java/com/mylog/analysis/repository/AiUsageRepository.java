package com.mylog.analysis.repository;

import com.mylog.analysis.entity.AnalysisJob;
import com.mylog.analysis.port.AiAnalysisOutput;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AiUsageRepository {

    private final JdbcTemplate jdbc;
    private final Clock clock;

    public AiUsageRepository(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    public void recordSuccess(AnalysisJob job, AiAnalysisOutput output, Instant now) {
        jdbc.update("""
                INSERT INTO ai_usage_records (
                    id, user_id, journal_entry_id, job_id, provider, model, operation,
                    input_token_count, output_token_count, latency_ms, success, error_code, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, NULL, ?)
                """, UUID.randomUUID(), job.userId(), job.journalId(), job.id(), output.provider(), output.model(),
                job.jobType(), output.inputTokens(), output.outputTokens(), output.latencyMs(), Timestamp.from(now));
    }

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
}
