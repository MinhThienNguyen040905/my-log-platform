package com.mylog.statistics.dto;

import com.mylog.statistics.service.MoodStatisticsView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MoodStatisticsResponse(
        LocalDate from, LocalDate to, String timezone, String calculationVersion,
        String trend, BigDecimal currentAverage, BigDecimal previousAverage,
        List<DailyMood> days, List<DayOfWeekMood> dayOfWeekPattern) {

    public static MoodStatisticsResponse from(MoodStatisticsView view) {
        return new MoodStatisticsResponse(
                view.from(), view.to(), view.timezone(), view.calculationVersion(), view.trend(),
                view.currentAverage(), view.previousAverage(),
                view.days().stream().map(item -> new DailyMood(
                        item.date(), item.journalCount(), item.moodAverage(),
                        item.stressAverage(), item.energyAverage())).toList(),
                view.dayOfWeekPattern().stream().map(item -> new DayOfWeekMood(
                        item.dayOfWeek(), item.sampleSize(), item.moodAverage())).toList());
    }

    public record DailyMood(
            LocalDate date, int journalCount, BigDecimal moodAverage,
            BigDecimal stressAverage, BigDecimal energyAverage) {}

    public record DayOfWeekMood(int dayOfWeek, int sampleSize, BigDecimal moodAverage) {}
}
