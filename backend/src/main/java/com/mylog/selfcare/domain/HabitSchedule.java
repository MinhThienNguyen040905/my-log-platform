package com.mylog.selfcare.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record HabitSchedule(String type, Set<Integer> daysOfWeek) {
    public HabitSchedule {
        if ("DAILY".equals(type)) {
            if (daysOfWeek != null && !daysOfWeek.isEmpty()) throw new IllegalArgumentException("daily days");
            daysOfWeek = Set.of();
        } else if ("WEEKLY".equals(type)) {
            if (daysOfWeek == null || daysOfWeek.isEmpty() || daysOfWeek.size() > 7
                    || daysOfWeek.stream().anyMatch(day -> day == null || day < 1 || day > 7))
                throw new IllegalArgumentException("weekly days");
            daysOfWeek = Set.copyOf(daysOfWeek);
        } else throw new IllegalArgumentException("frequency type");
    }

    public boolean scheduled(LocalDate date) {
        return "DAILY".equals(type) || daysOfWeek.contains(date.getDayOfWeek().getValue());
    }

    public int streak(LocalDate today, List<LocalDate> completedDates) {
        Set<LocalDate> completed = Set.copyOf(completedDates);
        LocalDate cursor = today;
        while (!scheduled(cursor)) cursor = cursor.minusDays(1);
        if (!completed.contains(cursor)) {
            cursor = cursor.minusDays(1);
            while (!scheduled(cursor)) cursor = cursor.minusDays(1);
        }
        int count = 0;
        while (count < 3660 && completed.contains(cursor)) {
            count++;
            cursor = cursor.minusDays(1);
            while (!scheduled(cursor)) cursor = cursor.minusDays(1);
        }
        return count;
    }
}
