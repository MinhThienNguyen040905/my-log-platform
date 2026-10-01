package com.mylog.selfcare.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "habit_completions")
class HabitCompletion {
    @Id UUID id;
    @Column(name = "habit_id", nullable = false) UUID habitId;
    @Column(name = "user_id", nullable = false) UUID userId;
    @Column(name = "local_date", nullable = false) LocalDate localDate;
    @Column(nullable = false) BigDecimal value;
    @Column(nullable = false) String source;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;
}
