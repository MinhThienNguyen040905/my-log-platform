package com.mylog.journal.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "companion_habit_preferences")
public class CompanionHabitPreference {
    @Id
    @Column(name = "user_id", nullable = false)
    public UUID userId;

    @Column(name = "last_suggested_date")
    public LocalDate lastSuggestedDate;

    @Column(name = "dismissed_until")
    public Instant dismissedUntil;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
