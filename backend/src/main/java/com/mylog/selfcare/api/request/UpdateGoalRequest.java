package com.mylog.selfcare.api.request;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateGoalRequest(@Size(max = 160) String title, @Size(max = 2000) String description,
                                String status, LocalDate startDate, LocalDate targetDate) {}
