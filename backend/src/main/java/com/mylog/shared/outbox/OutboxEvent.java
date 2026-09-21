package com.mylog.shared.outbox;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record OutboxEvent(
        UUID id,
        UUID aggregateId,
        String eventType,
        int eventVersion,
        Map<String, Object> payload,
        int attemptCount,
        Instant occurredAt) {

    public MessageEnvelope envelope() {
        return new MessageEnvelope(
                id, eventType, eventVersion, occurredAt, aggregateId, payload);
    }
}
