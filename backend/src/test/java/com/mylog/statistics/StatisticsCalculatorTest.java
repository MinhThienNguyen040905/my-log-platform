package com.mylog.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import com.mylog.statistics.repository.StatisticsRepository.DailyMoodRow;
import com.mylog.statistics.service.StatisticsCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class StatisticsCalculatorTest {

    private final StatisticsCalculator calculator = new StatisticsCalculator();

    @Test
    void preservesMissingValuesAndWeightsDailyAveragesByJournalCount() {
        BigDecimal result = calculator.weightedMoodAverage(List.of(
                new DailyMoodRow(LocalDate.of(2026, 1, 1), 1, new BigDecimal("10.00"), null, null),
                new DailyMoodRow(LocalDate.of(2026, 1, 2), 3, new BigDecimal("4.00"), null, null)));

        assertThat(result).isEqualByComparingTo("5.50");
        assertThat(calculator.weightedMoodAverage(List.of())).isNull();
    }

    @Test
    void classifiesTrendWithStableTolerance() {
        assertThat(calculator.trend(new BigDecimal("6.10"), new BigDecimal("6.00"))).isEqualTo("STABLE");
        assertThat(calculator.trend(new BigDecimal("7.00"), new BigDecimal("6.00"))).isEqualTo("UP");
        assertThat(calculator.trend(new BigDecimal("5.00"), new BigDecimal("6.00"))).isEqualTo("DOWN");
        assertThat(calculator.trend(null, new BigDecimal("6.00"))).isEqualTo("INSUFFICIENT_DATA");
    }
}
