package com.mylog.platform.outbox;

import java.util.UUID;

/** Feature-owned handler; payload remains restricted to aggregate ID and content version. */
public interface OutboxHandler {
    boolean supports(String eventType);
    void handle(String eventType, UUID aggregateId, int contentVersion);
}
