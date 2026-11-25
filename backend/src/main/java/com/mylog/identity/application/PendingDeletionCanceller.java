package com.mylog.identity.application;

import java.time.Instant;
import java.util.UUID;

/**
 * Cancels a user's account deletion request that is still in its grace period. Implemented by the module that owns
 * deletion requests, so identity never reads that table. Runs in the caller's transaction.
 */
public interface PendingDeletionCanceller {
    /** @return true when a request in its grace period, not yet due at {@code now}, was cancelled */
    boolean cancel(UUID userId, Instant now);
}
