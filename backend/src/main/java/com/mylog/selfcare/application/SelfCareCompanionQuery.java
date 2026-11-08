package com.mylog.selfcare.application;

import com.mylog.selfcare.application.query.CompanionHabitCandidate;

import java.util.Optional;
import java.util.UUID;

/**
 * Public read-only query port exposed by the {@code selfcare} module for the writing companion.
 * The {@code selfcare} module only provides habit data and mood-correlation facts; the companion's
 * own suggestion rules (frequency caps, mood exclusions, priority order) live in {@code journal}.
 */
public interface SelfCareCompanionQuery {
    Optional<CompanionHabitCandidate> findUncompletedActiveHabitByToolCode(UUID userId, String toolCode);

    Optional<CompanionHabitCandidate> findUncompletedPositiveMoodAssociatedHabit(UUID userId);
}
