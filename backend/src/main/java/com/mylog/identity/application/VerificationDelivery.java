package com.mylog.identity.application;

import java.time.Instant;

public interface VerificationDelivery {
    void send(String email, String token, String code);
    default void sendPasswordChangedAlert(String email, Instant changedAt) {}
    default void sendPasswordReset(String email, String token, String code) {}
    default void sendOAuthAccountNotice(String email) {}
}
