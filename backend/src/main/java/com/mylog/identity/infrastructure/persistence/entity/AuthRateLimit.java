package com.mylog.identity.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "auth_rate_limits")
public class AuthRateLimit {
    @Id @Column(name = "subject_hash") public byte[] subjectHash;
    @Column(name = "attempts", nullable = false) public int attempts;
    @Column(name = "window_started_at", nullable = false) public Instant windowStartedAt;
    @Column(name = "blocked_until") public Instant blockedUntil;
}
