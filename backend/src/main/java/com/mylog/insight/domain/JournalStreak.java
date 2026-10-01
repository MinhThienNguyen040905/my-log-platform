package com.mylog.insight.domain;

import com.mylog.insight.application.query.DailyMetric;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class JournalStreak {
    private JournalStreak() {}

    public record Result(int current, int longestInRange) {}

    /** A journal day has at least one saved journal; check-in alone does not count. */
    public static Result calculate(List<DailyMetric> days, LocalDate today) {
        Set<LocalDate> journalDates = new HashSet<>();
        for (DailyMetric day : days) if (day.journalCount() > 0) journalDates.add(day.date());
        int longest = 0;
        int run = 0;
        LocalDate previous = null;
        for (LocalDate date : journalDates.stream().sorted().toList()) {
            run = previous != null && previous.plusDays(1).equals(date) ? run + 1 : 1;
            longest = Math.max(longest, run);
            previous = date;
        }
        LocalDate cursor = journalDates.contains(today) ? today : today.minusDays(1);
        int current = 0;
        while (journalDates.contains(cursor)) {
            current++;
            cursor = cursor.minusDays(1);
        }
        return new Result(current, longest);
    }
}
