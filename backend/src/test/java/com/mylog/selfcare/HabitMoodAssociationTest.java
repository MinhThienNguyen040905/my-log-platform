package com.mylog.selfcare;

import com.mylog.selfcare.domain.HabitMoodAssociation;
import com.mylog.selfcare.domain.HabitSchedule;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

class HabitMoodAssociationTest {
    @Test void appearsOnlyWithSevenSamplesPerGroupAndNeverClaimsCausation() {
        LocalDate start = LocalDate.of(2026, 9, 1);
        var schedule = new HabitSchedule("DAILY", Set.of());
        var dates = IntStream.range(0, 14).mapToObj(start::plusDays).toList();
        var done = new HashSet<>(dates.subList(0, 7));
        var samples = IntStream.range(0, 14).mapToObj(i -> new HabitMoodAssociation.Day(
                dates.get(i), BigDecimal.valueOf(i < 7 ? 7 : 5))).toList();
        assertNull(HabitMoodAssociation.calculate(schedule, Set.copyOf(done), samples.subList(0, 13)));
        var result = HabitMoodAssociation.calculate(schedule, Set.copyOf(done), samples);
        assertNotNull(result);
        assertEquals(7, result.completedDays());
        assertEquals(7, result.otherDays());
        assertEquals(0, result.averageMoodDifference().compareTo(BigDecimal.valueOf(2)));
    }
}
