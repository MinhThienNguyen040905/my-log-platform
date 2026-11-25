package com.mylog.identity.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Decides whether a correctly authenticated account may sign in. An {@code ACTIVE} account signs in as usual. An
 * account waiting for deletion signs in only if its request is still in the grace period: signing in cancels the
 * request and reactivates the account (UC-23). Every other status is refused like a wrong password.
 */
@Component
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class AccountReactivation {
    private final IdentityStore store;
    private final PendingDeletionCanceller canceller;

    public AccountReactivation(IdentityStore store, PendingDeletionCanceller canceller) {
        this.store = store;
        this.canceller = canceller;
    }

    /**
     * @return true when this sign-in cancelled a pending account deletion
     * @throws IdentityService.InvalidCredentialsException when the account may not sign in
     */
    public boolean admit(IdentityStore.Account account, Instant now) {
        if ("ACTIVE".equals(account.status())) return false;
        if ("DELETION_PENDING".equals(account.status()) && canceller.cancel(account.id(), now)) {
            store.setStatus(account.id(), "ACTIVE", now);
            store.audit(account.id(), "ACCOUNT_DELETION_CANCELLED", account.id(), now);
            return true;
        }
        throw new IdentityService.InvalidCredentialsException();
    }
}
