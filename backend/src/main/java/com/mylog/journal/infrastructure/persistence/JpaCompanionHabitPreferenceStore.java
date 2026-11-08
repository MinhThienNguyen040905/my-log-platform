package com.mylog.journal.infrastructure.persistence;

import com.mylog.journal.application.CompanionHabitPreferenceStore;
import com.mylog.journal.infrastructure.persistence.entity.CompanionHabitPreference;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JpaCompanionHabitPreferenceStore implements CompanionHabitPreferenceStore {
    private final EntityManager em;

    JpaCompanionHabitPreferenceStore(EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<Preference> find(UUID userId) {
        CompanionHabitPreference row = em.find(CompanionHabitPreference.class, userId);
        if (row == null) return Optional.empty();
        return Optional.of(new Preference(row.userId, row.lastSuggestedDate, row.dismissedUntil, row.updatedAt));
    }

    @Override
    public void recordSuggestedOn(UUID userId, LocalDate localDate, Instant now) {
        em.createNativeQuery("""
                insert into companion_habit_preferences (user_id, last_suggested_date, dismissed_until, updated_at)
                values (:userId, :localDate, null, :now)
                on conflict (user_id) do update
                set last_suggested_date = excluded.last_suggested_date,
                    updated_at = excluded.updated_at
                """)
                .setParameter("userId", userId)
                .setParameter("localDate", localDate)
                .setParameter("now", now)
                .executeUpdate();
        em.clear();
    }

    @Override
    public void dismissUntil(UUID userId, Instant dismissedUntil, Instant now) {
        em.createNativeQuery("""
                insert into companion_habit_preferences (user_id, last_suggested_date, dismissed_until, updated_at)
                values (:userId, null, :dismissedUntil, :now)
                on conflict (user_id) do update
                set dismissed_until = excluded.dismissed_until,
                    updated_at = excluded.updated_at
                """)
                .setParameter("userId", userId)
                .setParameter("dismissedUntil", dismissedUntil)
                .setParameter("now", now)
                .executeUpdate();
        em.clear();
    }
}
