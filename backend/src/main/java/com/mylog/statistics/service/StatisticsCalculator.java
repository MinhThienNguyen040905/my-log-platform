package com.mylog.statistics.service;

import com.mylog.statistics.repository.StatisticsRepository.DailyMoodRow;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class StatisticsCalculator {

    public BigDecimal weightedMoodAverage(List<DailyMoodRow> rows) {
        BigDecimal weighted = BigDecimal.ZERO;
        int samples = 0;
        for (DailyMoodRow row : rows) {
            if (row.moodAverage() != null && row.journalCount() > 0) {
                weighted = weighted.add(row.moodAverage().multiply(BigDecimal.valueOf(row.journalCount())));
                samples += row.journalCount();
            }
        }
        return samples == 0 ? null : weighted.divide(BigDecimal.valueOf(samples), 2, RoundingMode.HALF_UP);
    }

    public String trend(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null) return "INSUFFICIENT_DATA";
        int comparison = current.subtract(previous).abs().compareTo(new BigDecimal("0.25"));
        if (comparison < 0) return "STABLE";
        return current.compareTo(previous) > 0 ? "UP" : "DOWN";
    }

    public BigDecimal ratio(BigDecimal value, BigDecimal total) {
        if (value == null || total == null || total.signum() == 0) return BigDecimal.ZERO;
        return value.divide(total, 4, RoundingMode.HALF_UP);
    }

    public BigDecimal ratio(int value, int total) {
        return total == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(value).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
    }
}
