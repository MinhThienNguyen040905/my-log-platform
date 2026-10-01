package com.mylog.analysis.infrastructure.persistence;

import com.mylog.analysis.application.AnalysisJobHandler;
import com.mylog.analysis.application.AnalysisOutputValidator;
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
class AiJobWorker {
    private final EntityManager em;
    private final TransactionTemplate tx;
    private final AnalysisJobHandler handler;
    private final Clock clock;
    private final MeterRegistry metrics;
    private final String workerId = UUID.randomUUID().toString();
    private final AtomicLong queueDepth = new AtomicLong();
    private final AtomicLong oldestAge = new AtomicLong();

    AiJobWorker(EntityManager em, TransactionTemplate tx, AnalysisJobHandler handler,
                Clock clock, MeterRegistry metrics) {
        this.em = em; this.tx = tx; this.handler = handler; this.clock = clock; this.metrics = metrics;
        metrics.gauge("mylog.ai.queue.depth", queueDepth);
        metrics.gauge("mylog.ai.queue.oldest.age.seconds", oldestAge);
    }

    @Scheduled(fixedDelayString = "${mylog.jobs.poll-delay-ms:2000}")
    void poll() {
        for (int i = 0; i < 10; i++) {
            UUID id = tx.execute(s -> claim());
            if (id == null) return;
            AiJob job = tx.execute(s -> em.find(AiJob.class, id));
            int version = Integer.parseInt(job.payload.get("contentVersion").toString());
            try {
                AnalysisJobHandler.Outcome outcome = handler.process(id, workerId, job.userId, job.aggregateId, version);
                tx.executeWithoutResult(s -> complete(id, outcome));
                metrics.counter("mylog.ai.jobs.completed", "outcome", outcome.name()).increment();
            } catch (RuntimeException exception) {
                boolean permanent = exception instanceof AnalysisOutputValidator.InvalidAnalysisOutputException;
                boolean terminal = tx.execute(s -> fail(id, permanent));
                if (terminal) handler.terminalFailure(job.userId, job.aggregateId, version);
                metrics.counter("mylog.ai.jobs.errors", "kind", permanent ? "INVALID_OUTPUT" : "TRANSIENT").increment();
            }
        }
    }

    private UUID claim() {
        Instant now = clock.instant();
        @SuppressWarnings("unchecked")
        List<UUID> ids = em.createNativeQuery("""
                SELECT id FROM ai_jobs WHERE
                    (status IN ('PENDING','RETRY_WAIT') AND available_at<=?1)
                    OR (status='PROCESSING' AND lease_expires_at<?1)
                ORDER BY priority, available_at, created_at FOR UPDATE SKIP LOCKED LIMIT 1
                """, UUID.class).setParameter(1, now).getResultList();
        if (ids.isEmpty()) return null;
        UUID id = ids.getFirst();
        em.createNativeQuery("""
                UPDATE ai_jobs SET status='PROCESSING', attempt=attempt+1,
                    started_at=coalesce(started_at,?2), locked_at=?2,
                    locked_by=?3, lease_expires_at=?4 WHERE id=?1
                """).setParameter(1, id).setParameter(2, now).setParameter(3, workerId)
                .setParameter(4, now.plusSeconds(300)).executeUpdate();
        return id;
    }

    private void complete(UUID id, AnalysisJobHandler.Outcome outcome) {
        AiJob job = em.find(AiJob.class, id);
        if (!"PROCESSING".equals(job.status) || !workerId.equals(job.lockedBy)) return;
        job.status = switch (outcome) {
            case SUCCEEDED, STALE -> "SUCCEEDED";
            case POLICY_BLOCKED, CONSENT_REQUIRED -> "FAILED";
        };
        job.lastErrorCode = switch (outcome) {
            case SUCCEEDED, STALE -> null;
            case POLICY_BLOCKED -> "POLICY_BLOCKED";
            case CONSENT_REQUIRED -> "CONSENT_REQUIRED";
        };
        job.finishedAt = clock.instant();
        job.lockedAt = null; job.lockedBy = null; job.leaseExpiresAt = null;
    }

    private boolean fail(UUID id, boolean permanent) {
        AiJob job = em.find(AiJob.class, id);
        if (!"PROCESSING".equals(job.status) || !workerId.equals(job.lockedBy)) return false;
        boolean terminal = permanent || job.attempt >= job.maxAttempts;
        job.status = terminal ? "DEAD" : "RETRY_WAIT";
        job.lastErrorCode = permanent ? "INVALID_OUTPUT" : "PROVIDER_UNAVAILABLE";
        job.lastErrorSummary = null;
        long delay = Math.min(3600, 1L << Math.min(job.attempt, 10));
        job.availableAt = clock.instant().plusSeconds(delay + ThreadLocalRandom.current().nextLong(delay / 4 + 1));
        job.finishedAt = terminal ? clock.instant() : null;
        job.lockedAt = null; job.lockedBy = null; job.leaseExpiresAt = null;
        return terminal;
    }

    @Scheduled(fixedDelayString = "${mylog.jobs.metrics-delay-ms:60000}")
    void queueMetrics() {
        Object[] state = (Object[]) em.createNativeQuery("""
                SELECT count(*), coalesce(extract(epoch from (?1 - min(created_at))),0)
                FROM ai_jobs WHERE status IN ('PENDING','RETRY_WAIT')
                """).setParameter(1, clock.instant()).getSingleResult();
        queueDepth.set(((Number) state[0]).longValue());
        oldestAge.set(((Number) state[1]).longValue());
    }
}
