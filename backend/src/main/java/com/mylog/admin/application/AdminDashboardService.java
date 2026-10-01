package com.mylog.admin.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class AdminDashboardService {
    private final AdminDashboardStore store;
    public AdminDashboardService(AdminDashboardStore store) { this.store=store; }
    @Transactional(readOnly = true)
    public View get() {
        var counts=store.counts();
        if (counts.activeUsers()<20) return new View(true,null,null,null,20);
        Long journal=counts.journalContributors()>=20 ? counts.journalEntries() : null;
        Long checkins=counts.checkinContributors()>=20 ? counts.dailyCheckins() : null;
        return new View(journal==null || checkins==null,counts.activeUsers(),journal,checkins,20);
    }
    public record View(boolean suppressed, Long activeUsers, Long journalEntries,
                       Long dailyCheckins, int minimumCohortSize) {}
}
