package com.mylog.identity.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Same condition as {@code SmtpVerificationDelivery}: without identity there is no delivery bean to inject. */
@Component
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class PasswordResetEventListener {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetEventListener.class);
    private final VerificationDelivery delivery;

    public PasswordResetEventListener(VerificationDelivery delivery) {
        this.delivery = delivery;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        try {
            delivery.sendPasswordReset(event.email(), event.token(), event.code());
        } catch (Exception e) {
            log.error("Failed to send password reset email to user={}", event.userId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOAuthResetAttempt(OAuthPasswordResetAttemptEvent event) {
        try {
            delivery.sendOAuthAccountNotice(event.email());
        } catch (Exception e) {
            log.error("Failed to send oauth reset notice to user={}", event.userId(), e);
        }
    }
}

