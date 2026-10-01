package com.mylog.reporting;

import com.mylog.reporting.domain.ReportPeriod;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportPeriodTest {
    @Test void localMondayAfterDstChangeUsesCalendarDates() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-29T22:30:00Z"), ZoneId.of("UTC"));
        LocalDate berlinToday = LocalDate.now(clock.withZone(ZoneId.of("Europe/Berlin")));
        assertEquals(LocalDate.of(2026, 3, 30), berlinToday);
        assertEquals(new ReportPeriod(LocalDate.of(2026, 3, 23), LocalDate.of(2026, 3, 29)),
                ReportPeriod.previousWeek(berlinToday));
    }

    @Test void previousMonthHandlesLeapDay() {
        assertEquals(new ReportPeriod(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29)),
                ReportPeriod.previousMonth(LocalDate.of(2024, 3, 1)));
    }
}
