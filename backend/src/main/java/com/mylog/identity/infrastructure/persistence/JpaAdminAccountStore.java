package com.mylog.identity.infrastructure.persistence;

import com.mylog.identity.application.AdminAccountStore;
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
class JpaAdminAccountStore implements AdminAccountStore {
    private final EntityManager em;
    private final IdGenerator ids;
    JpaAdminAccountStore(EntityManager em, IdGenerator ids) { this.em=em; this.ids=ids; }
    public List<Metadata> list(UUID beforeId, int limit) {
        return em.createQuery("select u from User u where u.deletedAt is null and (:before is null or u.id<:before) "
                        + "order by u.id desc", User.class)
                .setParameter("before",beforeId).setMaxResults(limit).getResultList().stream().map(this::metadata).toList();
    }
    public Optional<Metadata> get(UUID id) {
        User user=em.find(User.class,id);
        return user==null || user.deletedAt!=null ? Optional.empty() : Optional.of(metadata(user));
    }
    public boolean changeStatus(UUID id, String expected, String next, Instant now) {
        int count=em.createQuery("update User u set u.status=:next,u.updatedAt=:now,u.rowVersion=u.rowVersion+1 "
                        + "where u.id=:id and u.status=:expected and u.deletedAt is null")
                .setParameter("next",next).setParameter("now",now).setParameter("id",id)
                .setParameter("expected",expected).executeUpdate();
        em.clear(); return count==1;
    }
    public void revokeSessions(UUID id, Instant now) {
        em.createQuery("update AuthSession s set s.revokedAt=:now,s.revokeReason='ADMIN_SUSPEND' "
                        + "where s.userId=:user and s.revokedAt is null")
                .setParameter("now",now).setParameter("user",id).executeUpdate();
    }
    public void audit(UUID actor, String action, UUID target, String reasonCode, Instant now) {
        AuditLog log=new AuditLog(); log.id=ids.next(); log.actorUserId=actor; log.actorType="ADMIN";
        log.action=action; log.targetType="USER"; log.targetId=target; log.reasonCode=reasonCode;
        log.occurredAt=now; em.persist(log);
    }
    private Metadata metadata(User user) {
        return new Metadata(user.id,user.status,user.createdAt,user.lastLoginAt,user.rowVersion);
    }
}
