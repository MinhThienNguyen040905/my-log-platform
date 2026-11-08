package com.mylog.selfcare.application.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Candidate habit exposed to the journal companion via {@code SelfCareCompanionQuery}.
 * {@code todayLocalDate} is computed in the habit's own timezone so the frontend can log
 * completion without relying on the browser's clock.
 */
public record CompanionHabitCandidate(UUID habitId, String title, BigDecimal targetValue,
                                      String unit, String toolCode, LocalDate todayLocalDate,
                                      BigDecimal moodDifference) {}
