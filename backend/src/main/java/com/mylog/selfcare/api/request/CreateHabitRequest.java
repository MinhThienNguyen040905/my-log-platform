package com.mylog.selfcare.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record CreateHabitRequest(@NotBlank @Size(max = 160) String title, @NotNull BigDecimal targetValue,
                                 @NotBlank String unit, @NotBlank String frequencyType, List<Integer> daysOfWeek,
                                 @Size(max = 32) String toolCode) {
    public CreateHabitRequest(String title, BigDecimal targetValue, String unit, String frequencyType, List<Integer> daysOfWeek) {
        this(title, targetValue, unit, frequencyType, daysOfWeek, null);
    }
}
