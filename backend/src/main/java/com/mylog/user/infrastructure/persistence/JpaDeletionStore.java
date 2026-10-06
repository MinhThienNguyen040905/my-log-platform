package com.mylog.user.infrastructure.persistence;

import com.mylog.user.infrastructure.persistence.entity.DeletionRequest;

import com.mylog.user.application.DeletionStore;
import com.mylog.user.application.query.DeletionView;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JpaDeletionStore implements DeletionStore {
    private final EntityManager em;
    JpaDeletionStore(EntityManager em) {this.em=em;}
    @Override public DeletionView create(UUID id, UUID userId, byte[] hash, Instant now, Instant scheduled) {
        DeletionRequest request=new DeletionRequest();
        request.id=id;request.userId=userId;request.subjectHash=hash;request.status="GRACE_PERIOD";
        request.requestedAt=now;request.scheduledFor=scheduled;request.checkpoint="REQUESTED";
        em.persist(request);em.flush();return view(request);
    }
    @Override public Optional<DeletionView> find(UUID userId, UUID id) {
        return em.createQuery("select r from DeletionRequest r where r.id=:id and r.userId=:user",DeletionRequest.class)
                .setParameter("id",id).setParameter("user",userId).getResultStream().findFirst().map(this::view);
    }
    @Override public boolean cancel(UUID userId, UUID id, Instant now) {
        int updated=em.createNativeQuery("""
                UPDATE deletion_requests SET status='CANCELLED', cancelled_at=:now
                WHERE id=:id AND user_id=:user AND status='GRACE_PERIOD' AND scheduled_for>:now
                """).setParameter("id",id).setParameter("user",userId).setParameter("now",now).executeUpdate();
        return updated==1;
    }
    @Override public Optional<Work> claim(Instant now) {
        var ids=em.createNativeQuery("""
                SELECT id FROM deletion_requests WHERE
                    (status='GRACE_PERIOD' AND scheduled_for<=:now)
                    OR (status='FAILED' AND scheduled_for<=:now)
                    OR (status='PROCESSING' AND lease_expires_at<:now)
                ORDER BY scheduled_for LIMIT 1 FOR UPDATE SKIP LOCKED
                """,UUID.class).setParameter("now",now).getResultList();
        if (ids.isEmpty()) return Optional.empty();
        DeletionRequest request=em.find(DeletionRequest.class,ids.getFirst());
        if (request.userId==null) return Optional.empty();
        request.status="PROCESSING";request.attempt++;request.leaseExpiresAt=now.plusSeconds(300);
        return Optional.of(new Work(request.id,request.userId,request.checkpoint,request.attempt));
    }
    @Override public void checkpoint(UUID id,String checkpoint) {
        DeletionRequest request=em.find(DeletionRequest.class,id);
        if (request!=null) request.checkpoint=checkpoint;
    }
    @Override public void complete(UUID id,Instant now) {
        DeletionRequest request=em.find(DeletionRequest.class,id);
        request.userId=null;request.status="COMPLETED";request.checkpoint="COMPLETED";
        request.completedAt=now;request.leaseExpiresAt=null;request.lastErrorCode=null;
    }
    @Override public void fail(UUID id,Instant retryAt) {
        DeletionRequest request=em.find(DeletionRequest.class,id);
        if (request==null || !"PROCESSING".equals(request.status)) return;
        request.status="FAILED";request.lastErrorCode="CLEANUP_RETRY";
        request.scheduledFor=retryAt;request.leaseExpiresAt=null;
    }
    @Override public void purgeAudit(Instant cutoff) {
        em.createNativeQuery("""
                DELETE FROM deletion_requests WHERE
                    (status='COMPLETED' AND completed_at<:cutoff)
                    OR (status='CANCELLED' AND cancelled_at<:cutoff)
                """).setParameter("cutoff",cutoff).executeUpdate();
    }
    private DeletionView view(DeletionRequest request) {
        return new DeletionView(request.id,request.status,request.requestedAt,request.scheduledFor,request.completedAt);
    }
}
