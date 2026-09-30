package com.mylog.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_action_tokens")
class AuthActionTokenEntity {
    @Id UUID id;
    @Column(name = "user_id", nullable = false) UUID userId;
    @Column(name = "token_hash", nullable = false) byte[] tokenHash;
    @Column(name = "purpose", nullable = false) String purpose;
    @Column(name = "expires_at", nullable = false) Instant expiresAt;
    @Column(name = "consumed_at") Instant consumedAt;
    @Column(name = "created_at", nullable = false) Instant createdAt;
}
