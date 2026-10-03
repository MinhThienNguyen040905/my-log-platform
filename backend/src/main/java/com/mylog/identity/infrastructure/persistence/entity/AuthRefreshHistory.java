package com.mylog.identity.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_refresh_history")
public class AuthRefreshHistory {
    @Id public UUID id;
    @Column(name = "token_hash", nullable = false, unique = true) public byte[] tokenHash;
    @Column(name = "session_id", nullable = false) public UUID sessionId;
    @Column(name = "used_at", nullable = false) public Instant usedAt;
}
