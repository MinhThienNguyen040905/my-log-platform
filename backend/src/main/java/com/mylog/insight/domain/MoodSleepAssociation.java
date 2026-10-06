package com.mylog.insight.domain;

import com.mylog.insight.application.query.DailyMetric;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class MoodSleepAssociation {
    private MoodSleepAssociation() {}

    public record Result(String direction, BigDecimal strength, BigDecimal correlation,
                         int sampleSize, BigDecimal averageMood, BigDecimal averageSleepHours,
                         List<DailyMetric> evidence) {}

    public static Result calculate(List<DailyMetric> days, int minimumSamples) {
        List<DailyMetric> paired = days.stream()
                .filter(day -> day.moodScore() != null && day.sleepMinutes() != null).toList();
        if (paired.size() < minimumSamples) return null;
        double meanMood = paired.stream().mapToDouble(d -> d.moodScore().doubleValue()).average().orElseThrow();
        double meanSleep = paired.stream().mapToDouble(d -> d.sleepMinutes().doubleValue()).average().orElseThrow();
        double covariance = 0, moodVariance = 0, sleepVariance = 0;
        for (DailyMetric day : paired) {
            double mood = day.moodScore().doubleValue() - meanMood;
            double sleep = day.sleepMinutes() - meanSleep;
            covariance += mood * sleep;
            moodVariance += mood * mood;
            sleepVariance += sleep * sleep;
        }
        if (moodVariance == 0 || sleepVariance == 0) return null;
        double coefficient = Math.max(-1, Math.min(1, covariance / Math.sqrt(moodVariance * sleepVariance)));
        String direction = coefficient > 0.10 ? "UP" : coefficient < -0.10 ? "DOWN" : "STABLE";
        return new Result(direction, number(Math.abs(coefficient)), number(coefficient), paired.size(),
                number(meanMood), number(meanSleep / 60), paired);
    }

    private static BigDecimal number(double value) {
        return BigDecimal.valueOf(value).setScale(5, RoundingMode.HALF_UP);
    }
}
