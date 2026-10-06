package com.mylog.analysis.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdminJobStore {
    record Metadata(UUID id, UUID userId, String type, String status, int attempt,
                    String errorCode, Instant createdAt, Instant availableAt) {}
    record RetryTarget(UUID userId, UUID entryId, int contentVersion) {}
    List<Metadata> list(UUID beforeId, int limit);
    Optional<RetryTarget> retry(UUID jobId, Instant now);
    void audit(UUID actor, UUID jobId, Instant now);
}
