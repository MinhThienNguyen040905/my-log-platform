package com.mylog.admin.infrastructure.persistence;

import com.mylog.admin.application.AdminDashboardStore;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JpaAdminDashboardStore implements AdminDashboardStore {
    private final EntityManager em;
    JpaAdminDashboardStore(EntityManager em) { this.em=em; }
    public Counts counts() {
        Object[] row=(Object[]) em.createNativeQuery("""
                select (select count(*) from users where status='ACTIVE' and deleted_at is null),
                       (select count(distinct user_id) from journal_entries where deleted_at is null),
                       (select count(*) from journal_entries where deleted_at is null),
                       (select count(distinct user_id) from daily_checkins),
                       (select count(*) from daily_checkins)
                """).getSingleResult();
        return new Counts(((Number)row[0]).longValue(),((Number)row[1]).longValue(),
                ((Number)row[2]).longValue(),((Number)row[3]).longValue(),((Number)row[4]).longValue());
    }
}
