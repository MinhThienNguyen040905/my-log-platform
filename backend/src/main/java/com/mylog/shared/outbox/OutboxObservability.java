package com.mylog.shared.outbox;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import com.mylog.shared.messaging.MessagingTopology;
import java.time.Clock;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class OutboxObservability implements HealthIndicator {

    private final OutboxRepository repository;
    private final RabbitAdmin rabbitAdmin;
    private final Clock clock;

    public OutboxObservability(
            OutboxRepository repository,
            RabbitAdmin rabbitAdmin,
            Clock clock,
            MeterRegistry registry) {
        this.repository = repository;
        this.rabbitAdmin = rabbitAdmin;
        this.clock = clock;
        Gauge.builder("outbox.pending.count", repository, this::safePendingCount)
                .description("Outbox events not yet published")
                .register(registry);
        Gauge.builder("outbox.oldest.pending.age", repository, ignored -> safeOldestAge())
                .baseUnit("seconds")
                .description("Age of the oldest unpublished outbox event")
                .register(registry);
        Gauge.builder("messaging.dead.letter.depth", rabbitAdmin, this::safeDeadLetterDepth)
                .description("Messages currently waiting in the dead-letter queue")
                .register(registry);
        queueDepthGauge(registry, MessagingTopology.JOURNAL_EVENTS_QUEUE);
        queueDepthGauge(registry, MessagingTopology.ANALYSIS_QUEUE);
    }

    @Override
    public Health health() {
        try {
            return Health.up()
                    .withDetail("pendingEvents", repository.pendingCount())
                    .withDetail("oldestPendingAgeSeconds", repository.oldestPendingAgeSeconds(clock.instant()))
                    .withDetail("deadLetterDepth", safeDeadLetterDepth(rabbitAdmin))
                    .withDetail("journalQueueDepth", safeQueueDepth(MessagingTopology.JOURNAL_EVENTS_QUEUE))
                    .withDetail("analysisQueueDepth", safeQueueDepth(MessagingTopology.ANALYSIS_QUEUE))
                    .build();
        } catch (RuntimeException exception) {
            return Health.down().withException(exception).build();
        }
    }

    private double safePendingCount(OutboxRepository ignored) {
        try {
            return repository.pendingCount();
        } catch (RuntimeException exception) {
            return Double.NaN;
        }
    }

    private double safeOldestAge() {
        try {
            return repository.oldestPendingAgeSeconds(clock.instant());
        } catch (RuntimeException exception) {
            return Double.NaN;
        }
    }

    private double safeDeadLetterDepth(RabbitAdmin ignored) {
        return safeQueueDepth(MessagingTopology.DEAD_LETTER_QUEUE);
    }

    private void queueDepthGauge(MeterRegistry registry, String queue) {
        Gauge.builder("messaging.queue.depth", rabbitAdmin, ignored -> safeQueueDepth(queue))
                .tag("queue", queue)
                .description("Messages waiting in a RabbitMQ queue")
                .register(registry);
    }

    private double safeQueueDepth(String queue) {
        try {
            QueueInformation information = rabbitAdmin.getQueueInfo(queue);
            return information == null ? 0 : information.getMessageCount();
        } catch (RuntimeException exception) {
            return Double.NaN;
        }
    }
}
