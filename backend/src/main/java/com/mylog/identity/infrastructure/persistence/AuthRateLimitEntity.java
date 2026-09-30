package com.mylog.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "auth_rate_limits")
class AuthRateLimitEntity {
    @Id @Column(name = "subject_hash") byte[] subjectHash;
    @Column(name = "attempts", nullable = false) int attempts;
    @Column(name = "window_started_at", nullable = false) Instant windowStartedAt;
    @Column(name = "blocked_until") Instant blockedUntil;
}
