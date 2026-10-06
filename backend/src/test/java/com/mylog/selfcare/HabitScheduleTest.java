package com.mylog.selfcare;

import com.mylog.selfcare.domain.HabitSchedule;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class HabitScheduleTest {
    @Test void weeklyStreakSkipsUnscheduledDaysAndAllowsCurrentDueDateUntilCompleted() {
        var mondayWednesday = new HabitSchedule("WEEKLY", Set.of(1, 3));
        LocalDate wednesday = LocalDate.of(2026, 10, 7);
        assertEquals(2, mondayWednesday.streak(wednesday, List.of(wednesday.minusDays(2), wednesday.minusDays(7))));
        assertEquals(3, mondayWednesday.streak(wednesday, List.of(wednesday, wednesday.minusDays(2), wednesday.minusDays(7))));
        assertFalse(mondayWednesday.scheduled(wednesday.plusDays(1)));
    }
    @Test void dailyStreakStopsAtMissedDueDay() {
        var daily = new HabitSchedule("DAILY", Set.of());
        LocalDate today = LocalDate.of(2026, 10, 5);
        assertEquals(2, daily.streak(today, List.of(today, today.minusDays(1), today.minusDays(3))));
        assertThrows(IllegalArgumentException.class, () -> new HabitSchedule("WEEKLY", Set.of()));
    }
}
