package com.mylog.user.application;

import com.mylog.identity.application.PendingDeletionCanceller;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Signing in during the grace period cancels the account deletion request (UC-23). Active whether or not
 * account deletion is enabled, so requests made before the feature was switched off can still be cancelled.
 */
@Component
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class DeletionCancellationOnSignIn implements PendingDeletionCanceller {
    private final DeletionStore store;

    DeletionCancellationOnSignIn(DeletionStore store) { this.store = store; }

    @Override public boolean cancel(UUID userId, Instant now) { return store.cancelInGracePeriod(userId, now); }
}
