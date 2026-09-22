package com.mylog.common.outbox;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxScheduler {

    private final OutboxPublisher publisher;
    private final OutboxProperties properties;

    public OutboxScheduler(OutboxPublisher publisher, OutboxProperties properties) {
        this.publisher = publisher;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${mylog.outbox.poll-interval:1s}")
    public void publish() {
        if (properties.schedulingEnabled()) {
            publisher.publishAvailable();
        }
    }
}
