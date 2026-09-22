package com.mylog.common.outbox;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

@Service
public class OutboxPublisher {

    private final OutboxRepository repository;
    private final OutboxTransport transport;
    private final OutboxProperties properties;
    private final Clock clock;
    private final Counter failureCounter;
    private final Counter retryCounter;
    private final Counter deadLetterCounter;

    public OutboxPublisher(
            OutboxRepository repository,
            OutboxTransport transport,
            OutboxProperties properties,
            Clock clock,
            MeterRegistry meterRegistry) {
        this.repository = repository;
        this.transport = transport;
        this.properties = properties;
        this.clock = clock;
        this.failureCounter = Counter.builder("outbox.publish.failed")
                .description("Outbox publish attempts that failed")
                .register(meterRegistry);
        this.retryCounter = Counter.builder("outbox.retry.scheduled")
                .description("Outbox publish retries scheduled")
                .register(meterRegistry);
        this.deadLetterCounter = Counter.builder("outbox.dead.letter")
                .description("Outbox events routed to the dead-letter queue")
                .register(meterRegistry);
    }

    public int publishAvailable() {
        Instant now = clock.instant();
        List<OutboxEvent> batch = repository.claimBatch(
                properties.batchSize(),
                properties.maxAttempts(),
                now,
                now.plus(properties.claimLease()));
        for (OutboxEvent event : batch) {
            publishOne(event);
        }
        return batch.size();
    }

    private void publishOne(OutboxEvent event) {
        try {
            transport.publish(event.envelope());
            repository.markPublished(event.id(), clock.instant());
        } catch (RuntimeException exception) {
            failureCounter.increment();
            String errorCode = sanitizedErrorCode(exception);
            if (event.attemptCount() >= properties.maxAttempts()) {
                deadLetter(event, errorCode);
            } else {
                retryCounter.increment();
                repository.markRetry(
                        event.id(), clock.instant().plus(retryDelay(event.attemptCount())), errorCode);
            }
        }
    }

    private void deadLetter(OutboxEvent event, String errorCode) {
        try {
            transport.deadLetter(event.envelope(), errorCode);
            repository.markDeadLettered(event.id(), clock.instant(), errorCode);
            deadLetterCounter.increment();
        } catch (RuntimeException deadLetterFailure) {
            repository.markDeadLetterUnavailable(
                    event.id(), clock.instant().plus(properties.retryMaxDelay()));
        }
    }

    private Duration retryDelay(int attempt) {
        long baseMillis = properties.retryBaseDelay().toMillis();
        long maxMillis = properties.retryMaxDelay().toMillis();
        int exponent = Math.min(20, Math.max(0, attempt - 1));
        long exponential;
        try {
            exponential = Math.multiplyExact(baseMillis, 1L << exponent);
        } catch (ArithmeticException exception) {
            exponential = maxMillis;
        }
        long capped = Math.min(maxMillis, exponential);
        long jitterBound = Math.max(1, capped / 4);
        return Duration.ofMillis(Math.min(maxMillis, capped + ThreadLocalRandom.current().nextLong(jitterBound)));
    }

    private String sanitizedErrorCode(RuntimeException exception) {
        String name = exception.getClass().getSimpleName();
        return name.isBlank() ? "PUBLISH_FAILED" : name.substring(0, Math.min(100, name.length()));
    }
}
