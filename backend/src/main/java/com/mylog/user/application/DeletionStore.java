package com.mylog.user.application;

import com.mylog.user.application.query.DeletionView;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface DeletionStore {
    DeletionView create(UUID id, UUID userId, byte[] subjectHash, Instant now, Instant scheduledFor);
    /** Cancels the user's request still in its grace period and not yet due. @return true when one was cancelled */
    boolean cancelInGracePeriod(UUID userId, Instant now);
    Optional<Work> claim(Instant now);
    void complete(UUID id, Instant now);
    void fail(UUID id, Instant retryAt, String errorCode);
    void purgeAudit(Instant cutoff);
    record Work(UUID id, UUID userId, int attempt) {}
}
