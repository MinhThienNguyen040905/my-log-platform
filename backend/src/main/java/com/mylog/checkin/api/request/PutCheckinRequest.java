package com.mylog.checkin.api.request;

import com.mylog.checkin.application.command.PutCheckinCommand;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public record PutCheckinRequest(@NotBlank String timezone, String moodCode, BigDecimal moodScore,
                                BigDecimal stressScore, BigDecimal energyScore, Integer sleepMinutes,
                                String note, List<ActivityRequest> activities) {
    public record ActivityRequest(String code, Integer durationMinutes, String intensity) {}

    /** {@code activities} omitted (null) keeps the day's existing activities; {@code []} clears them. */
    public PutCheckinCommand command() {
        return new PutCheckinCommand(timezone, moodCode, moodScore, stressScore, energyScore,
                sleepMinutes, note, activities == null ? null : activities.stream()
                .map(a -> a == null ? null : new PutCheckinCommand.ActivityCommand(
                        a.code(), a.durationMinutes(), a.intensity())).toList());
    }
}
