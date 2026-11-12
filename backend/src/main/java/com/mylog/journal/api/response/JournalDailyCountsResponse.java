package com.mylog.journal.api.response;

import com.mylog.journal.application.query.DailyEntryCountView;

import java.time.LocalDate;
import java.util.List;

/** Calendar view: only days that have saved entries, oldest first. */
public record JournalDailyCountsResponse(LocalDate from, LocalDate to, List<Day> days) {
    public record Day(LocalDate date, int count, String latestMoodCode) {}

    public static JournalDailyCountsResponse from(LocalDate from, LocalDate to, List<DailyEntryCountView> views) {
        return new JournalDailyCountsResponse(from, to,
                views.stream().map(v -> new Day(v.date(), v.count(), v.latestMoodCode())).toList());
    }
}
