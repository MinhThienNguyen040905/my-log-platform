package com.mylog.selfcare.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "habit_completions")
public class HabitCompletion {
    @Id public UUID id;
    @Column(name = "habit_id", nullable = false) public UUID habitId;
    @Column(name = "user_id", nullable = false) public UUID userId;
    @Column(name = "local_date", nullable = false) public LocalDate localDate;
    @Column(nullable = false) public BigDecimal value;
    @Column(nullable = false) public String source;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
    @Column(name = "updated_at", nullable = false) public Instant updatedAt;
}
