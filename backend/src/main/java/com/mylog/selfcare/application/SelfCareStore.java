package com.mylog.selfcare.application;

import com.mylog.platform.crypto.SensitiveDataCipher.Encrypted;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface SelfCareStore {
    record Goal(UUID id, UUID userId, String category, Encrypted title, Encrypted description,
                String status, LocalDate startDate, LocalDate targetDate, Instant createdAt,
                Instant updatedAt, Instant completedAt, long version) {}
    record Habit(UUID id, UUID goalId, UUID userId, Encrypted title, BigDecimal targetValue,
                 String unit, String frequencyType, Map<String, Object> frequencyConfig, String timezone,
                 String status, Instant createdAt, Instant updatedAt, long version) {}
    record Completion(UUID id, UUID habitId, UUID userId, LocalDate localDate, BigDecimal value,
                      String source, Instant createdAt, Instant updatedAt) {}

    void createGoal(Goal goal);
    Optional<Goal> goal(UUID userId, UUID id);
    List<Goal> goals(UUID userId);
    boolean updateGoal(Goal goal, long expectedVersion);
    void createHabit(Habit habit);
    Optional<Habit> habit(UUID userId, UUID id);
    List<Habit> habits(UUID userId, UUID goalId);
    List<Completion> completions(UUID userId, UUID habitId, LocalDate from, LocalDate to);
    List<Completion> exportAllCompletions(UUID userId);
    void putCompletion(Completion completion);
    void deleteCompletion(UUID userId, UUID habitId, LocalDate date);
}
