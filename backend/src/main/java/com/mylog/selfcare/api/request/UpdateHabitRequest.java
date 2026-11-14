package com.mylog.selfcare.api.request;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Partial update: a {@code null} field keeps the stored value. {@code toolCode} set to an empty string clears the
 * linked tool; {@code daysOfWeek} is only read together with the effective {@code frequencyType}.
 */
public record UpdateHabitRequest(@Size(max = 160) String title, BigDecimal targetValue, String unit,
                                 String frequencyType, List<Integer> daysOfWeek, @Size(max = 32) String toolCode,
                                 String status) {}
