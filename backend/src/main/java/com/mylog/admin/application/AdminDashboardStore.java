package com.mylog.admin.application;

public interface AdminDashboardStore {
    record Counts(long activeUsers, long journalContributors, long journalEntries,
                  long checkinContributors, long dailyCheckins) {}
    Counts counts();
}
