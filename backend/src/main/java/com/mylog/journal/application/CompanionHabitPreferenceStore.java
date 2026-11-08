package com.mylog.journal.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence port for the companion's per-user habit suggestion cadence and 7-day dismissal state.
 * Owned by the {@code journal} module (see COMPANION_CHATBOT_PLAN.md stage 5).
 */
public interface CompanionHabitPreferenceStore {
    record Preference(UUID userId, LocalDate lastSuggestedDate, Instant dismissedUntil, Instant updatedAt) {}

    Optional<Preference> find(UUID userId);

    void recordSuggestedOn(UUID userId, LocalDate localDate, Instant now);

    void dismissUntil(UUID userId, Instant dismissedUntil, Instant now);
}
