package com.mylog.identity.application;

import java.time.Instant;

public interface VerificationDelivery {
    void send(String email, String token, String code);
    default void sendPasswordChangedAlert(String email, Instant changedAt) {}
    default void sendPasswordReset(String email, String token, String code) {}
    default void sendOAuthAccountNotice(String email) {}
    /** Account deletion was requested and will run at {@code scheduledFor} unless the user signs in again. */
    default void sendAccountDeletionScheduled(String email, Instant scheduledFor) {}
    /** The account and its data have been deleted. Sent after the deletion commits. */
    default void sendAccountDeleted(String email) {}
}
