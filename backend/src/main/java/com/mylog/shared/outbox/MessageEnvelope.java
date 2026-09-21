package com.mylog.shared.outbox;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MessageEnvelope(
        UUID messageId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        UUID aggregateId,
        Map<String, Object> payload) {}
