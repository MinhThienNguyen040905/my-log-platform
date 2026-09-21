package com.mylog.shared.outbox;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class OutboxWriter {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxWriter(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public UUID append(
            String aggregateType,
            UUID aggregateId,
            String eventType,
            int eventVersion,
            Map<String, Object> payload) {
        Instant now = clock.instant();
        UUID eventId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO outbox_events (
                    id, aggregate_type, aggregate_id, event_type, event_version,
                    payload, status, attempt_count, next_attempt_at, occurred_at)
                VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), 'PENDING', 0, ?, ?)
                """,
                eventId,
                aggregateType,
                aggregateId,
                eventType,
                eventVersion,
                objectMapper.writeValueAsString(payload),
                Timestamp.from(now),
                Timestamp.from(now));
        return eventId;
    }
}
