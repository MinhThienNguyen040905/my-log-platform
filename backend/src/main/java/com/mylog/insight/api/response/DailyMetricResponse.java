package com.mylog.insight.api.response;

import com.mylog.insight.application.query.DailyMetric;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyMetricResponse(LocalDate date, String source, int journalCount,
                                  BigDecimal moodScore, BigDecimal stressScore,
                                  BigDecimal energyScore, Integer sleepMinutes) {
    public static DailyMetricResponse from(DailyMetric value) {
        return new DailyMetricResponse(value.date(), value.source(), value.journalCount(),
                value.moodScore(), value.stressScore(), value.energyScore(), value.sleepMinutes());
    }
}
