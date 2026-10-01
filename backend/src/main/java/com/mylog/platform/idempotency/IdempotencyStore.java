package com.mylog.platform.idempotency;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyStore {
    Optional<UUID> reserve(UUID userId, String operation, String key, byte[] requestHash, Instant now);
    void complete(UUID userId, String operation, String key, UUID reference, int status, Instant now);
}
