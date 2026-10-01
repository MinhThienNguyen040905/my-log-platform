package com.mylog.insight.api.response;

import com.mylog.insight.application.query.DashboardView;

import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(String range, String timezone, LocalDate from, LocalDate to,
                                List<DailyMetricResponse> timeline, List<TopCodeResponse> topEmotions,
                                List<TopCodeResponse> topTopics, int currentJournalStreak,
                                int longestJournalStreak, int journalEntryCount, int moodSampleSize) {
    public static DashboardResponse from(DashboardView view) {
        return new DashboardResponse(view.range(), view.timezone(), view.from(), view.to(),
                view.timeline().stream().map(DailyMetricResponse::from).toList(),
                view.topEmotions().stream().map(TopCodeResponse::from).toList(),
                view.topTopics().stream().map(TopCodeResponse::from).toList(),
                view.currentJournalStreak(), view.longestJournalStreak(),
                view.journalEntryCount(), view.moodSampleSize());
    }
}
