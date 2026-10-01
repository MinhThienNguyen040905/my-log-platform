package com.mylog.analysis.infrastructure.persistence;

import com.mylog.analysis.application.AnalysisRetryStore;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JpaAnalysisRetryStore implements AnalysisRetryStore {
    private final EntityManager em;

    JpaAnalysisRetryStore(EntityManager em) { this.em = em; }

    @Override public State requeue(UUID userId, UUID entryId, int version, Instant now) {
        String key = "journal-analysis:" + entryId + ":" + version;
        AiJob job = em.createQuery("""
                select j from AiJob j where j.idempotencyKey=:key and j.userId=:userId
                """, AiJob.class).setParameter("key", key).setParameter("userId", userId)
                .getResultStream().findFirst().orElse(null);
        if (job == null) return State.MISSING;
        em.lock(job, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if ("PENDING".equals(job.status) || "RETRY_WAIT".equals(job.status)
                || "PROCESSING".equals(job.status)) return State.ALREADY_QUEUED;
        if (!"DEAD".equals(job.status) && !"FAILED".equals(job.status)) return State.ALREADY_QUEUED;
        if (job.finishedAt != null && job.finishedAt.isAfter(now.minusSeconds(60))) return State.TOO_SOON;
        job.status = "PENDING";
        job.attempt = 0;
        job.availableAt = now;
        job.lastErrorCode = null;
        job.lastErrorSummary = null;
        job.finishedAt = null;
        job.lockedAt = null; job.lockedBy = null; job.leaseExpiresAt = null;
        em.flush();
        return State.QUEUED;
    }
}
