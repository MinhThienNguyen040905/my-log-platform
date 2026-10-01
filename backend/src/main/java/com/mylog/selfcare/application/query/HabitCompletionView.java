package com.mylog.selfcare.application.query;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record HabitCompletionView(UUID id,UUID habitId,LocalDate localDate,BigDecimal value,
                                  String source,Instant createdAt,Instant updatedAt) {}
