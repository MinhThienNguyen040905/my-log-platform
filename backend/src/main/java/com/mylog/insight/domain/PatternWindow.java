package com.mylog.insight.domain;

import com.mylog.insight.application.query.ActivitySignal;
import com.mylog.insight.application.query.DailyMetric;
import com.mylog.insight.application.query.EntrySignal;

import java.time.LocalDate;
import java.util.List;

/** The structured data of one local-date window (inclusive), as read for pattern detection. */
public record PatternWindow(LocalDate from, LocalDate to, List<DailyMetric> days, List<EntrySignal> entries,
                            List<ActivitySignal> activities) {
    public PatternWindow {
        days = List.copyOf(days);
        entries = List.copyOf(entries);
        activities = List.copyOf(activities);
    }

    public static PatternWindow empty(LocalDate from, LocalDate to) {
        return new PatternWindow(from, to, List.of(), List.of(), List.of());
    }
}
