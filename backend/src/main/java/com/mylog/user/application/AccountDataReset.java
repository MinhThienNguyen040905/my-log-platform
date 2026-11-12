package com.mylog.user.application;

import java.time.Instant;
import java.util.UUID;

/** Deletes everything a user has written or that was derived from it, keeping the account, profile and sign-ins. */
public interface AccountDataReset {
    /** Locks the user row so writes that start during the reset wait for it; call inside the reset transaction. */
    void lock(UUID userId);
    void reset(UUID userId, Instant now);
}
