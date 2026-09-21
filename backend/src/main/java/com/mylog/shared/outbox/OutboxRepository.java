package com.mylog.shared.outbox;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Repository
public class OutboxRepository {

    private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {};

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public OutboxRepository(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public List<OutboxEvent> claimBatch(
            int batchSize, int maxAttempts, Instant now, Instant leaseUntil) {
        return transactionTemplate.execute(status -> jdbcTemplate.query(
                """
                WITH candidates AS (
                    SELECT id
                    FROM outbox_events
                    WHERE ((status IN ('PENDING', 'FAILED')
                            AND next_attempt_at <= ? AND attempt_count < ?)
                           OR (status = 'PUBLISHING'
                               AND next_attempt_at <= ? AND attempt_count <= ?))
                    ORDER BY occurred_at, id
                    FOR UPDATE SKIP LOCKED
                    LIMIT ?
                )
                UPDATE outbox_events event
                SET status = 'PUBLISHING',
                    attempt_count = event.attempt_count + 1,
                    next_attempt_at = ?
                FROM candidates
                WHERE event.id = candidates.id
                RETURNING event.id, event.aggregate_id, event.event_type,
                          event.event_version, event.payload::text,
                          event.attempt_count, event.occurred_at
                """,
                (resultSet, rowNumber) -> new OutboxEvent(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getObject("aggregate_id", UUID.class),
                        resultSet.getString("event_type"),
                        resultSet.getInt("event_version"),
                        objectMapper.readValue(resultSet.getString("payload"), PAYLOAD_TYPE),
                        resultSet.getInt("attempt_count"),
                        resultSet.getTimestamp("occurred_at").toInstant()),
                Timestamp.from(now),
                maxAttempts,
                Timestamp.from(now),
                maxAttempts,
                batchSize,
                Timestamp.from(leaseUntil)));
    }

    public void markPublished(UUID id, Instant publishedAt) {
        jdbcTemplate.update(
                """
                UPDATE outbox_events
                SET status = 'PUBLISHED', published_at = ?, last_error_code = NULL
                WHERE id = ? AND status = 'PUBLISHING'
                """,
                Timestamp.from(publishedAt),
                id);
    }

    public void markRetry(UUID id, Instant nextAttemptAt, String errorCode) {
        jdbcTemplate.update(
                """
                UPDATE outbox_events
                SET status = 'FAILED', next_attempt_at = ?, last_error_code = ?
                WHERE id = ? AND status = 'PUBLISHING'
                """,
                Timestamp.from(nextAttemptAt),
                errorCode,
                id);
    }

    public void markDeadLettered(UUID id, Instant now, String errorCode) {
        jdbcTemplate.update(
                """
                UPDATE outbox_events
                SET status = 'FAILED', next_attempt_at = TIMESTAMPTZ '9999-12-31 23:59:59+00',
                    published_at = ?, last_error_code = ?
                WHERE id = ? AND status = 'PUBLISHING'
                """,
                Timestamp.from(now),
                errorCode,
                id);
    }

    public void markDeadLetterUnavailable(UUID id, Instant nextAttemptAt) {
        jdbcTemplate.update(
                """
                UPDATE outbox_events
                SET status = 'FAILED', next_attempt_at = ?,
                    attempt_count = GREATEST(0, attempt_count - 1),
                    last_error_code = 'DEAD_LETTER_UNAVAILABLE'
                WHERE id = ? AND status = 'PUBLISHING'
                """,
                Timestamp.from(nextAttemptAt),
                id);
    }

    public long pendingCount() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE published_at IS NULL",
                Long.class);
        return count == null ? 0 : count;
    }

    public double oldestPendingAgeSeconds(Instant now) {
        Instant oldest = jdbcTemplate.queryForObject(
                "SELECT min(occurred_at) FROM outbox_events WHERE published_at IS NULL",
                (resultSet, rowNumber) -> {
                    Timestamp value = resultSet.getTimestamp(1);
                    return value == null ? null : value.toInstant();
                });
        return oldest == null ? 0 : Math.max(0, now.getEpochSecond() - oldest.getEpochSecond());
    }
}
