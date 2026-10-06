package com.mylog.insight;

import com.mylog.insight.application.query.DailyMetric;
import com.mylog.insight.domain.JournalStreak;
import com.mylog.insight.domain.MoodSleepAssociation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InsightCalculationsTest {
    @Test void journalStreakIgnoresCheckinOnlyDaysAndCanEndYesterday() {
        LocalDate today = LocalDate.of(2026, 10, 5);
        var days = List.of(day(today.minusDays(3), 1, 4, 360),
                day(today.minusDays(2), 1, 5, 390),
                day(today.minusDays(1), 1, 6, 420),
                day(today, 0, 7, 450));
        var result = JournalStreak.calculate(days, today);
        assertEquals(3, result.current());
        assertEquals(3, result.longestInRange());
    }

    @Test void associationRequiresPairedSamplesAndVariation() {
        List<DailyMetric> days = new ArrayList<>();
        LocalDate start = LocalDate.of(2026, 1, 1);
        for (int i = 0; i < 7; i++) days.add(day(start.plusDays(i), 1, i + 2, 300 + i * 30));
        assertNull(MoodSleepAssociation.calculate(days, 8));
        var result = MoodSleepAssociation.calculate(days, 7);
        assertNotNull(result);
        assertEquals("UP", result.direction());
        assertEquals(7, result.sampleSize());
        assertEquals(0, BigDecimal.ONE.compareTo(result.strength()));
        assertNull(MoodSleepAssociation.calculate(days.stream()
                .map(d -> day(d.date(), 1, 5, d.sleepMinutes())).toList(), 7));
    }

    private static DailyMetric day(LocalDate date, int journals, int mood, int sleep) {
        return new DailyMetric(date, journals == 0 ? "CHECKIN" : "JOURNAL_FALLBACK",
                UUID.randomUUID(), journals, BigDecimal.valueOf(mood), null, null, sleep);
    }
}
