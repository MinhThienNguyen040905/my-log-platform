package com.mylog.selfcare.api.response;

import com.mylog.selfcare.application.query.GoalView;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GoalResponse(UUID id, String category, String title, String description, String status,
                           LocalDate startDate, LocalDate targetDate, long version,
                           Instant createdAt, Instant updatedAt, List<HabitResponse> habits) {
    public static GoalResponse from(GoalView view) {
        return new GoalResponse(view.id(), view.category(), view.title(), view.description(), view.status(),
                view.startDate(), view.targetDate(), view.version(), view.createdAt(), view.updatedAt(),
                view.habits().stream().map(HabitResponse::from).toList());
    }
}
