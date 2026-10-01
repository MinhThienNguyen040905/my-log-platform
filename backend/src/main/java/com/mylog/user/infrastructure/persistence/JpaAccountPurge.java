package com.mylog.user.infrastructure.persistence;

import com.mylog.user.application.AccountPurge;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JpaAccountPurge implements AccountPurge {
    private final EntityManager em;
    JpaAccountPurge(EntityManager em) {this.em=em;}
    @Override public void purge(UUID userId) {
        // Tables without user FK need explicit cleanup before the user cascade.
        em.createNativeQuery("""
                DELETE FROM outbox_events WHERE aggregate_type='JOURNAL_ENTRY'
                AND aggregate_id IN (SELECT id FROM journal_entries WHERE user_id=:user)
                """).setParameter("user",userId).executeUpdate();
        em.createNativeQuery("""
                DELETE FROM ai_usage_records WHERE job_id IN (SELECT id FROM ai_jobs WHERE user_id=:user)
                   OR analysis_id IN (SELECT id FROM ai_analyses WHERE user_id=:user)
                """).setParameter("user",userId).executeUpdate();
        em.createNativeQuery("DELETE FROM safety_events WHERE user_id=:user")
                .setParameter("user",userId).executeUpdate();
        em.createNativeQuery("DELETE FROM audit_logs WHERE actor_user_id=:user OR target_id=:user")
                .setParameter("user",userId).executeUpdate();
        int deleted=em.createNativeQuery("DELETE FROM users WHERE id=:user AND status='DELETION_PENDING'")
                .setParameter("user",userId).executeUpdate();
        if (deleted!=1) throw new IllegalStateException("Account purge precondition failed");
    }
}
