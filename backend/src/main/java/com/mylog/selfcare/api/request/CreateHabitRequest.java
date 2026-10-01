package com.mylog.selfcare.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record CreateHabitRequest(@NotBlank @Size(max = 160) String title, @NotNull BigDecimal targetValue,
                                 @NotBlank String unit, @NotBlank String frequencyType, List<Integer> daysOfWeek) {}
