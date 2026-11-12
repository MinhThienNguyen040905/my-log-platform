package com.mylog.user.infrastructure.persistence;

import com.mylog.platform.id.IdGenerator;
import com.mylog.user.application.AccountDataReset;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

/**
 * Native deletes in foreign-key order. Child rows (entry tags, analyses with their emotions/topics, check-in
 * activities, insight/report evidence, habit completions) go with their parents through ON DELETE CASCADE;
 * rows whose links are SET NULL (safety events, AI usage records) are deleted first. Draft images not yet
 * attached to an entry are deleted by owner.
 */
@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JpaAccountDataReset implements AccountDataReset {
    private static final String ENTRIES = "SELECT id FROM journal_entries WHERE user_id=:user";
    private static final String[] STATEMENTS = {
            "DELETE FROM outbox_events WHERE aggregate_type='JOURNAL_ENTRY' AND aggregate_id IN (" + ENTRIES + ")",
            "DELETE FROM ai_usage_records WHERE job_id IN (SELECT id FROM ai_jobs WHERE user_id=:user)"
                    + " OR analysis_id IN (SELECT id FROM ai_analyses WHERE user_id=:user)",
            "DELETE FROM safety_events WHERE user_id=:user OR journal_entry_id IN (" + ENTRIES + ")",
            "DELETE FROM reports WHERE user_id=:user",
            "DELETE FROM insights WHERE user_id=:user",
            "DELETE FROM ai_jobs WHERE user_id=:user OR (aggregate_type='JOURNAL_ENTRY' AND aggregate_id IN (" + ENTRIES + "))",
            "DELETE FROM journal_assets WHERE user_id=:user",
            "DELETE FROM journal_entries WHERE user_id=:user",
            "DELETE FROM journal_tags WHERE user_id=:user",
            "DELETE FROM daily_checkins WHERE user_id=:user",
            "DELETE FROM habit_completions WHERE user_id=:user",
            "DELETE FROM habits WHERE user_id=:user",
            "DELETE FROM selfcare_goals WHERE user_id=:user",
            "DELETE FROM companion_habit_preferences WHERE user_id=:user",
            "DELETE FROM export_requests WHERE user_id=:user",
            "DELETE FROM feedback WHERE user_id=:user",
            "DELETE FROM idempotency_keys WHERE user_id=:user",
    };
    private final EntityManager em;
    private final IdGenerator ids;

    JpaAccountDataReset(EntityManager em, IdGenerator ids) { this.em = em; this.ids = ids; }

    @Override public void lock(UUID userId) {
        // FOR UPDATE also blocks inserts that reference the user (their FK check takes FOR KEY SHARE).
        em.createNativeQuery("SELECT id FROM users WHERE id=:user FOR UPDATE").setParameter("user", userId).getResultList();
    }

    @Override public void reset(UUID userId, Instant now) {
        for (String statement : STATEMENTS) em.createNativeQuery(statement).setParameter("user", userId).executeUpdate();
        // The audit row records that a reset happened, never what was deleted.
        em.createNativeQuery("""
                INSERT INTO audit_logs(id,actor_user_id,actor_type,action,target_type,target_id,occurred_at)
                VALUES (:id,:user,'USER','DATA_RESET','USER',:user,:now)
                """).setParameter("id", ids.next()).setParameter("user", userId).setParameter("now", now).executeUpdate();
    }
}
