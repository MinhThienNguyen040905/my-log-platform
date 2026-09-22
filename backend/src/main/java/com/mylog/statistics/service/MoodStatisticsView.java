package com.mylog.statistics.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MoodStatisticsView(
        LocalDate from,
        LocalDate to,
        String timezone,
        String calculationVersion,
        String trend,
        BigDecimal currentAverage,
        BigDecimal previousAverage,
        List<DailyMood> days,
        List<DayOfWeekMood> dayOfWeekPattern) {

    public record DailyMood(
            LocalDate date, int journalCount, BigDecimal moodAverage,
            BigDecimal stressAverage, BigDecimal energyAverage) {}

    public record DayOfWeekMood(int dayOfWeek, int sampleSize, BigDecimal moodAverage) {}
}
