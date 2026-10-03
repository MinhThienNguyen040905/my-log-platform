package com.mylog.identity.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id public UUID id;
    @Column(name = "email_lookup_hash", nullable = false) public byte[] emailLookupHash;
    @Column(name = "encrypted_email", nullable = false) public byte[] encryptedEmail;
    @Column(name = "email_iv", nullable = false) public byte[] emailIv;
    @Column(name = "email_wrapped_key", nullable = false) public byte[] emailWrappedKey;
    @Column(name = "email_key_version", nullable = false) public String emailKeyVersion;
    @Column(name = "password_hash", nullable = false) public String passwordHash;
    @Column(name = "auth_provider", nullable = false) public String authProvider;
    @Column(name = "status", nullable = false) public String status;
    @Column(name = "email_verified_at") public Instant emailVerifiedAt;
    @Column(name = "last_login_at") public Instant lastLoginAt;
    @Column(name = "failed_login_count", nullable = false) public int failedLoginCount;
    @Column(name = "locked_until") public Instant lockedUntil;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
    @Column(name = "updated_at", nullable = false) public Instant updatedAt;
    @Column(name = "deleted_at") public Instant deletedAt;
    @Column(name = "row_version", nullable = false) public long rowVersion;
}
