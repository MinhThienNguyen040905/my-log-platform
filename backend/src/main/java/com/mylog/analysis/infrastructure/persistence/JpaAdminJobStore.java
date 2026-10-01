package com.mylog.analysis.infrastructure.persistence;

import com.mylog.analysis.application.AdminJobStore;
import com.mylog.platform.id.IdGenerator;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JpaAdminJobStore implements AdminJobStore {
    private final EntityManager em;
    private final IdGenerator ids;
    JpaAdminJobStore(EntityManager em, IdGenerator ids) { this.em=em; this.ids=ids; }
    public List<Metadata> list(UUID beforeId, int limit) {
        return em.createQuery("select j from AiJob j where (:before is null or j.id<:before) order by j.id desc", AiJob.class)
                .setParameter("before",beforeId).setMaxResults(limit).getResultList().stream()
                .map(j -> new Metadata(j.id,j.userId,j.jobType,j.status,j.attempt,j.lastErrorCode,
                        j.createdAt,j.availableAt)).toList();
    }
    public Optional<RetryTarget> retry(UUID jobId, Instant now) {
        AiJob candidate=em.find(AiJob.class,jobId);
        if (candidate==null || candidate.userId==null || !"JOURNAL_ANALYSIS".equals(candidate.jobType)
                || !"JOURNAL_ENTRY".equals(candidate.aggregateType)
                || candidate.payload==null || !(candidate.payload.get("contentVersion") instanceof Number version))
            return Optional.empty();
        RetryTarget target=new RetryTarget(candidate.userId,candidate.aggregateId,version.intValue());
        int changed=em.createQuery("update AiJob j set j.status='PENDING',j.attempt=0,j.availableAt=:now, "
                        + "j.lastErrorCode=null,j.lastErrorSummary=null,j.finishedAt=null, "
                        + "j.lockedAt=null,j.lockedBy=null,j.leaseExpiresAt=null "
                        + "where j.id=:id and j.status='DEAD' and j.lastErrorCode='PROVIDER_UNAVAILABLE' "
                        + "and j.finishedAt<:cutoff")
                .setParameter("now",now).setParameter("id",jobId).setParameter("cutoff",now.minusSeconds(60))
                .executeUpdate();
        em.clear(); return changed==1 ? Optional.of(target) : Optional.empty();
    }
    public void audit(UUID actor, UUID jobId, Instant now) {
        em.createNativeQuery("""
                insert into audit_logs(id,actor_user_id,actor_type,action,target_type,target_id,occurred_at)
                values (:id,:actor,'ADMIN','AI_JOB_RETRIED','AI_JOB',:target,:now)
                """).setParameter("id",ids.next()).setParameter("actor",actor)
                .setParameter("target",jobId).setParameter("now",now).executeUpdate();
    }
}
