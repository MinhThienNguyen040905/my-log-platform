package com.mylog.identity.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_sessions")
public class AuthSession {
    @Id public UUID id;
    @Column(name = "user_id", nullable = false) public UUID userId;
    @Column(name = "token_family_id", nullable = false) public UUID tokenFamilyId;
    @Column(name = "current_token_hash", nullable = false) public byte[] currentTokenHash;
    @Column(name = "device_name") public String deviceName;
    @Column(name = "last_used_at", nullable = false) public Instant lastUsedAt;
    @Column(name = "expires_at", nullable = false) public Instant expiresAt;
    @Column(name = "revoked_at") public Instant revokedAt;
    @Column(name = "revoke_reason") public String revokeReason;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
}
