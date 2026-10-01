package com.mylog.user.application;

import com.mylog.user.application.query.DeletionView;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface DeletionStore {
    DeletionView create(UUID id, UUID userId, byte[] subjectHash, Instant now, Instant scheduledFor);
    Optional<DeletionView> find(UUID userId, UUID id);
    boolean cancel(UUID userId, UUID id, Instant now);
    Optional<Work> claim(Instant now);
    void checkpoint(UUID id, String checkpoint);
    void complete(UUID id, Instant now);
    void fail(UUID id, Instant retryAt);
    void purgeAudit(Instant cutoff);
    record Work(UUID id, UUID userId, String checkpoint, int attempt) {}
}
