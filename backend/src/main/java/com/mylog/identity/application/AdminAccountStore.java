package com.mylog.identity.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdminAccountStore {
    record Metadata(UUID id, String status, Instant createdAt, Instant lastLoginAt, long version) {}
    List<Metadata> list(UUID beforeId, int limit);
    Optional<Metadata> get(UUID id);
    boolean changeStatus(UUID id, String expected, String next, Instant now);
    void revokeSessions(UUID id, Instant now);
    void audit(UUID actor, String action, UUID target, String reasonCode, Instant now);
}
