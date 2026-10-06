package com.mylog.admin.api.response;

import com.mylog.admin.application.AdminDashboardService;

public record AdminDashboardResponse(boolean suppressed, Long activeUsers, Long journalEntries,
                                     Long dailyCheckins, int minimumCohortSize) {
    public static AdminDashboardResponse from(AdminDashboardService.View v) {
        return new AdminDashboardResponse(v.suppressed(),v.activeUsers(),v.journalEntries(),
                v.dailyCheckins(),v.minimumCohortSize());
    }
}
