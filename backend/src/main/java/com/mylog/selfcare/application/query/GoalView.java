package com.mylog.selfcare.application.query;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GoalView(UUID id, String category, String title, String description, String status,
                       LocalDate startDate, LocalDate targetDate, long version,
                       Instant createdAt, Instant updatedAt, List<HabitView> habits) {}
