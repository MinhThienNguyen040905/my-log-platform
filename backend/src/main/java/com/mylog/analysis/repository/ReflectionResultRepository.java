package com.mylog.analysis.repository;

import com.mylog.analysis.entity.AnalysisJob;
import com.mylog.analysis.port.AiAnalysisOutput;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReflectionResultRepository {

    private final JdbcTemplate jdbc;

    public ReflectionResultRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void replaceWith(AnalysisJob job, AiAnalysisOutput output, Instant now) {
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
}
