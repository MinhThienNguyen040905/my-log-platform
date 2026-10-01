package com.mylog.feedback.infrastructure.persistence;

import com.mylog.feedback.application.FeedbackStore;
import com.mylog.feedback.application.query.FeedbackView;
import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.platform.id.IdGenerator;
import com.mylog.platform.web.ResourceNotFoundException;
import com.mylog.platform.web.ConflictException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JpaFeedbackStore implements FeedbackStore {
    private final EntityManager em;
    private final SensitiveDataCipher cipher;
    private final IdGenerator ids;
    JpaFeedbackStore(EntityManager em,SensitiveDataCipher cipher,IdGenerator ids) {
        this.em=em;this.cipher=cipher;this.ids=ids;
    }
    @Override public void create(UUID id,UUID userId,String category,SensitiveDataCipher.Encrypted message,Instant now) {
        Feedback feedback=new Feedback();
        feedback.id=id;feedback.userId=userId;feedback.category=category;feedback.status="OPEN";
        feedback.encryptedMessage=message.ciphertext();feedback.messageIv=message.iv();
        feedback.messageWrappedKey=message.wrappedKey();feedback.messageKeyVersion=message.keyVersion();
        feedback.createdAt=now;feedback.updatedAt=now;feedback.expiresAt=now.plusSeconds(180L*86400);
        em.persist(feedback);
    }
    @Override public Optional<FeedbackView> find(UUID id,UUID userId,boolean admin) {
        return em.createQuery("select f from Feedback f where f.id=:id and (:admin=true or f.userId=:user)",Feedback.class)
                .setParameter("id",id).setParameter("admin",admin).setParameter("user",userId)
                .getResultStream().findFirst().map(this::view);
    }
    @Override public List<FeedbackView> list(String status,int limit) {
        return em.createQuery("select f from Feedback f where (:status is null or f.status=:status) order by f.createdAt desc",Feedback.class)
                .setParameter("status",status).setMaxResults(limit).getResultList().stream().map(this::view).toList();
    }
    @Override public List<FeedbackView> listOwn(UUID userId) {
        return em.createQuery("select f from Feedback f where f.userId=:user order by f.createdAt",Feedback.class)
                .setParameter("user",userId).getResultList().stream().map(this::view).toList();
    }
    @Override public void update(UUID id,String status,UUID assignee,long expectedVersion,Instant now) {
        Feedback feedback=em.find(Feedback.class,id);
        if (feedback==null) throw new ResourceNotFoundException("Feedback not found.");
        if (feedback.version!=expectedVersion) throw new ConflictException("Feedback has changed.");
        feedback.status=status;
        if (assignee!=null) feedback.assignedTo=assignee;
        feedback.updatedAt=now;
        feedback.resolvedAt="RESOLVED".equals(status)||"CLOSED".equals(status)?now:null;
        try {em.flush();}
        catch (OptimisticLockException e) {throw new ConflictException("Feedback has changed.");}
    }
    @Override public boolean canAssign(UUID userId) {
        Number count=(Number)em.createNativeQuery("""
                SELECT count(*) FROM user_roles ur JOIN roles r ON r.id=ur.role_id
                JOIN users u ON u.id=ur.user_id
                WHERE ur.user_id=:user AND u.status='ACTIVE'
                AND (ur.expires_at IS NULL OR ur.expires_at>CURRENT_TIMESTAMP)
                AND r.code IN ('SUPPORT_AGENT','SYSTEM_ADMIN')
                """).setParameter("user",userId).getSingleResult();
        return count.intValue()>0;
    }
    @Override public void audit(UUID actor,UUID target,String action,Instant now) {
        em.createNativeQuery("""
                INSERT INTO audit_logs(id,actor_user_id,actor_type,action,target_type,target_id,occurred_at)
                VALUES (:id,:actor,'USER',:action,'FEEDBACK',:target,:now)
                """).setParameter("id",ids.next()).setParameter("actor",actor).setParameter("action",action)
                .setParameter("target",target).setParameter("now",now).executeUpdate();
    }
    @Override public void expire(Instant now) {
        em.createQuery("delete from Feedback f where f.expiresAt<=:now").setParameter("now",now).executeUpdate();
    }
    private FeedbackView view(Feedback f) {
        String message=cipher.decrypt("feedback.message",f.userId,f.id,
                new SensitiveDataCipher.Encrypted(f.encryptedMessage,f.messageIv,f.messageWrappedKey,f.messageKeyVersion));
        return new FeedbackView(f.id,f.userId,f.category,f.status,message,f.assignedTo,
                f.createdAt,f.updatedAt,f.resolvedAt,f.version);
    }
}
