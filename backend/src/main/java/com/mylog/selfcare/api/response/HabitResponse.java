package com.mylog.selfcare.api.response;

import com.mylog.selfcare.application.query.HabitView;
import com.mylog.selfcare.application.query.MoodAssociationView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record HabitResponse(UUID id, UUID goalId, String title, BigDecimal targetValue, String unit,
                            String frequencyType, List<Integer> daysOfWeek, String timezone, String status,
                            long version, int streak, int completionCount, List<LocalDate> completedDates,
                            MoodAssociationView moodAssociation) {
    public static HabitResponse from(HabitView view) {
        return new HabitResponse(view.id(), view.goalId(), view.title(), view.targetValue(), view.unit(),
                view.frequencyType(), view.daysOfWeek(), view.timezone(), view.status(), view.version(),
                view.streak(), view.completionCount(), view.completedDates(), view.moodAssociation());
    }
}
