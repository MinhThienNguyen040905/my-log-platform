package com.mylog.identity.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_action_tokens")
public class AuthActionToken {
    @Id public UUID id;
    @Column(name = "user_id", nullable = false) public UUID userId;
    @Column(name = "token_hash", nullable = false) public byte[] tokenHash;
    @Column(name = "code_hash") public byte[] codeHash;
    @Column(name = "code_expires_at") public Instant codeExpiresAt;
    @Column(name = "code_failed_attempts", nullable = false) public int codeFailedAttempts;
    @Column(name = "purpose", nullable = false) public String purpose;
    @Column(name = "expires_at", nullable = false) public Instant expiresAt;
    @Column(name = "consumed_at") public Instant consumedAt;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
}
