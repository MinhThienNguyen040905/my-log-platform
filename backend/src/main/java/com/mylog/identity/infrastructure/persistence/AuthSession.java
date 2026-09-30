package com.mylog.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_sessions")
class AuthSession {
    @Id UUID id;
    @Column(name = "user_id", nullable = false) UUID userId;
    @Column(name = "token_family_id", nullable = false) UUID tokenFamilyId;
    @Column(name = "current_token_hash", nullable = false) byte[] currentTokenHash;
    @Column(name = "device_name") String deviceName;
    @Column(name = "last_used_at", nullable = false) Instant lastUsedAt;
    @Column(name = "expires_at", nullable = false) Instant expiresAt;
    @Column(name = "revoked_at") Instant revokedAt;
    @Column(name = "revoke_reason") String revokeReason;
    @Column(name = "created_at", nullable = false) Instant createdAt;
}
