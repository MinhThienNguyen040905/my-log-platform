package com.mylog.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
class User {
    @Id UUID id;
    @Column(name = "email_lookup_hash", nullable = false) byte[] emailLookupHash;
    @Column(name = "encrypted_email", nullable = false) byte[] encryptedEmail;
    @Column(name = "email_iv", nullable = false) byte[] emailIv;
    @Column(name = "email_wrapped_key", nullable = false) byte[] emailWrappedKey;
    @Column(name = "email_key_version", nullable = false) String emailKeyVersion;
    @Column(name = "password_hash", nullable = false) String passwordHash;
    @Column(name = "auth_provider", nullable = false) String authProvider;
    @Column(name = "status", nullable = false) String status;
    @Column(name = "email_verified_at") Instant emailVerifiedAt;
    @Column(name = "last_login_at") Instant lastLoginAt;
    @Column(name = "failed_login_count", nullable = false) int failedLoginCount;
    @Column(name = "locked_until") Instant lockedUntil;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;
    @Column(name = "deleted_at") Instant deletedAt;
    @Column(name = "row_version", nullable = false) long rowVersion;
}
