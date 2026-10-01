package com.mylog.selfcare.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public final class HabitMoodAssociation {
    private HabitMoodAssociation() {}
    public record Day(LocalDate date, BigDecimal mood) {}
    public record Result(int completedDays, int otherDays, BigDecimal averageMoodDifference) {}
    public static Result calculate(HabitSchedule schedule, Set<LocalDate> completed,
                                                List<Day> days) {
        var valid = days.stream().filter(d -> d.mood() != null && schedule.scheduled(d.date())).toList();
        var done = valid.stream().filter(d -> completed.contains(d.date())).toList();
        var other = valid.stream().filter(d -> !completed.contains(d.date())).toList();
        if (done.size() < 7 || other.size() < 7) return null;
        BigDecimal completedMean = mean(done);
        BigDecimal otherMean = mean(other);
        return new Result(done.size(), other.size(), completedMean.subtract(otherMean));
    }
    private static BigDecimal mean(List<Day> days) {
        return days.stream().map(Day::mood).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(days.size()), 2, RoundingMode.HALF_UP);
    }
}
