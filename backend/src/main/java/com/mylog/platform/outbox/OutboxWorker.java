package com.mylog.platform.outbox;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

@Component
@ConditionalOnProperty(prefix = "mylog.jobs", name = "enabled", havingValue = "true")
@ConditionalOnExpression("'${mylog.app-profile:all}' != 'api'")
public class OutboxWorker {
    private final EntityManager em;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final MeterRegistry metrics;
    private final List<OutboxHandler> handlers;
    private final String workerId = UUID.randomUUID().toString();
    private final AtomicLong queueDepth = new AtomicLong();
    private final AtomicLong oldestAge = new AtomicLong();

    public OutboxWorker(EntityManager em, TransactionTemplate tx, Clock clock,
                        MeterRegistry metrics, List<OutboxHandler> handlers) {
        this.em = em; this.tx = tx; this.clock = clock; this.metrics = metrics; this.handlers = handlers;
        metrics.gauge("mylog.outbox.queue.depth", queueDepth);
        metrics.gauge("mylog.outbox.oldest.age.seconds", oldestAge);
    }

    @Scheduled(fixedDelayString = "${mylog.jobs.poll-delay-ms:2000}")
    public void poll() {
        for (int i = 0; i < 20; i++) {
            UUID id = tx.execute(s -> claim());
            if (id == null) return;
            try {
                tx.executeWithoutResult(s -> dispatch(id));
                metrics.counter("mylog.outbox.published").increment();
            } catch (RuntimeException exception) {
                tx.executeWithoutResult(s -> fail(id));
                metrics.counter("mylog.outbox.errors").increment();
            }
        }
    }

    private UUID claim() {
        Instant now = clock.instant();
        @SuppressWarnings("unchecked")
        List<UUID> ids = em.createNativeQuery("""
                SELECT id FROM outbox_events
                WHERE (status IN ('PENDING','FAILED') AND available_at <= ?1)
                   OR (status='PROCESSING' AND lease_expires_at < ?1)
                ORDER BY created_at, id FOR UPDATE SKIP LOCKED LIMIT 1
                """, UUID.class).setParameter(1, now).getResultList();
        if (ids.isEmpty()) return null;
        UUID id = ids.getFirst();
        em.createNativeQuery("""
                UPDATE outbox_events SET status='PROCESSING', attempt=attempt+1,
                    locked_at=?2, locked_by=?3, lease_expires_at=?4
                WHERE id=?1
                """).setParameter(1, id).setParameter(2, now).setParameter(3, workerId)
                .setParameter(4, now.plusSeconds(60)).executeUpdate();
        return id;
    }

    private void dispatch(UUID id) {
        OutboxEvent event = em.find(OutboxEvent.class, id);
        if (!"PROCESSING".equals(event.status) || !workerId.equals(event.lockedBy)) return;
        int version = Integer.parseInt(event.payload.get("contentVersion").toString());
        OutboxHandler handler = handlers.stream().filter(h -> h.supports(event.eventType))
                .findFirst().orElseThrow(() -> new IllegalStateException("UNSUPPORTED_EVENT"));
        handler.handle(event.eventType, event.aggregateId, version);
        em.createNativeQuery("""
                UPDATE outbox_events SET status='PUBLISHED', published_at=?2,
                    locked_at=NULL, locked_by=NULL, lease_expires_at=NULL, last_error_code=NULL
                WHERE id=?1 AND status='PROCESSING' AND locked_by=?3
                """).setParameter(1, id).setParameter(2, clock.instant())
                .setParameter(3, workerId).executeUpdate();
    }

    private void fail(UUID id) {
        OutboxEvent event = em.find(OutboxEvent.class, id);
        if (!"PROCESSING".equals(event.status) || !workerId.equals(event.lockedBy)) return;
        boolean policyWait = "SafetyRescreenRequested".equals(event.eventType);
        event.status = !policyWait && event.attempt >= 10 ? "DEAD" : "FAILED";
        event.lastErrorCode = policyWait ? "POLICY_NOT_READY" : "DISPATCH_FAILED";
        long delay = Math.min(3600, 1L << Math.min(event.attempt, 10));
        event.availableAt = clock.instant().plusSeconds(delay + ThreadLocalRandom.current().nextLong(delay / 4 + 1));
        event.lockedAt = null;
        event.lockedBy = null;
        event.leaseExpiresAt = null;
    }

    @Scheduled(fixedDelayString = "${mylog.jobs.metrics-delay-ms:60000}")
    public void queueMetrics() {
        Object[] state = (Object[]) em.createNativeQuery("""
                SELECT count(*), coalesce(extract(epoch from (?1 - min(created_at))),0)
                FROM outbox_events WHERE status IN ('PENDING','FAILED')
                """).setParameter(1, clock.instant()).getSingleResult();
        queueDepth.set(((Number) state[0]).longValue());
        oldestAge.set(((Number) state[1]).longValue());
    }
}
