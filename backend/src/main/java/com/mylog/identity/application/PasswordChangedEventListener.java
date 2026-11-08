package com.mylog.identity.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PasswordChangedEventListener {
    private static final Logger log = LoggerFactory.getLogger(PasswordChangedEventListener.class);
    private final VerificationDelivery delivery;

    public PasswordChangedEventListener(VerificationDelivery delivery) {
        this.delivery = delivery;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordChanged(PasswordChangedEvent event) {
        try {
            delivery.sendPasswordChangedAlert(event.email(), event.changedAt());
        } catch (Exception e) {
            log.error("Failed to send password changed alert to user={}", event.userId(), e);
        }
    }
}

