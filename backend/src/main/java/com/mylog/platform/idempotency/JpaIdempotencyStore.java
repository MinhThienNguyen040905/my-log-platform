package com.mylog.platform.idempotency;

import com.mylog.platform.id.IdGenerator;
import com.mylog.platform.web.ConflictException;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JpaIdempotencyStore implements IdempotencyStore {
    private final EntityManager entityManager;
    private final IdGenerator ids;

    JpaIdempotencyStore(EntityManager entityManager, IdGenerator ids) {
        this.entityManager = entityManager;
        this.ids = ids;
    }

    @Override public Optional<UUID> reserve(UUID userId, String operation, String key, byte[] requestHash, Instant now) {
        int inserted = entityManager.createNativeQuery("""
                INSERT INTO idempotency_keys(id,user_id,operation,idempotency_key,request_hash,state,expires_at,created_at)
                VALUES (?1,?2,?3,?4,?5,'PROCESSING',?6,?7)
                ON CONFLICT (user_id,operation,idempotency_key) DO NOTHING
                """).setParameter(1, ids.next()).setParameter(2, userId).setParameter(3, operation)
                .setParameter(4, key).setParameter(5, requestHash)
                .setParameter(6, now.plusSeconds(86400)).setParameter(7, now).executeUpdate();
        if (inserted == 1) return Optional.empty();
        Object[] previous = (Object[]) entityManager.createNativeQuery("""
                SELECT request_hash,state,response_reference FROM idempotency_keys
                WHERE user_id=?1 AND operation=?2 AND idempotency_key=?3 FOR UPDATE
                """).setParameter(1, userId).setParameter(2, operation).setParameter(3, key).getSingleResult();
        if (!MessageDigest.isEqual(requestHash, (byte[]) previous[0]) || !"COMPLETED".equals(previous[1])
                || previous[2] == null) throw new ConflictException("Idempotency-Key đã được dùng cho yêu cầu khác.");
        return Optional.of((UUID) previous[2]);
    }

    @Override public void complete(UUID userId, String operation, String key, UUID reference, int status, Instant now) {
        entityManager.createQuery("""
                update IdempotencyKey k set k.state='COMPLETED', k.responseReference=:reference,
                  k.responseStatus=:status, k.completedAt=:now
                where k.userId=:userId and k.operation=:operation and k.idempotencyKey=:key and k.state='PROCESSING'
                """).setParameter("reference", reference).setParameter("status", status).setParameter("now", now)
                .setParameter("userId", userId).setParameter("operation", operation).setParameter("key", key).executeUpdate();
    }
}
