package com.mylog.selfcare.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateGoalRequest(@NotBlank String category, @NotBlank @Size(max = 160) String title,
                                @Size(max = 2000) String description, LocalDate startDate, LocalDate targetDate) {}
