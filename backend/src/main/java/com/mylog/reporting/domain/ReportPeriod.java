package com.mylog.reporting.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;

public record ReportPeriod(LocalDate start, LocalDate end) {
    public static ReportPeriod previousWeek(LocalDate localToday) {
        LocalDate thisMonday = localToday.with(DayOfWeek.MONDAY);
        return new ReportPeriod(thisMonday.minusWeeks(1), thisMonday.minusDays(1));
    }

    public static ReportPeriod previousMonth(LocalDate localToday) {
        YearMonth month = YearMonth.from(localToday).minusMonths(1);
        return new ReportPeriod(month.atDay(1), month.atEndOfMonth());
    }
}
