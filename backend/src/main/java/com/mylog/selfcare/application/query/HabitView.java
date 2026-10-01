package com.mylog.selfcare.application.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record HabitView(UUID id, UUID goalId, String title, BigDecimal targetValue, String unit,
                        String frequencyType, List<Integer> daysOfWeek, String timezone, String status,
                        long version, int streak, int completionCount, List<LocalDate> completedDates,
                        MoodAssociationView moodAssociation) {}
