package com.mylog.platform.outbox;

import java.time.Instant;
import java.util.UUID;

public interface OutboxPublisher {
    void publish(String aggregateType, UUID aggregateId, String eventType, int contentVersion, Instant now);
}
