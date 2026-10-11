package com.mylog.identity.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pending_registrations")
public class PendingRegistrationEntity {
    @Id public UUID id;
    @Column(name = "email_lookup_hash", nullable = false) public byte[] emailLookupHash;
    @Column(name = "encrypted_email", nullable = false) public byte[] encryptedEmail;
    @Column(name = "email_iv", nullable = false) public byte[] emailIv;
    @Column(name = "email_wrapped_key", nullable = false) public byte[] emailWrappedKey;
    @Column(name = "email_key_version", nullable = false) public String emailKeyVersion;
    @Column(name = "password_hash", nullable = false) public String passwordHash;
    @Column(nullable = false) public String timezone;
    @Column(nullable = false) public String locale;
    @Column(name = "terms_version", nullable = false) public String termsVersion;
    @Column(name = "privacy_version", nullable = false) public String privacyVersion;
    @Column(name = "token_hash", nullable = false) public byte[] tokenHash;
    @Column(name = "code_hash", nullable = false) public byte[] codeHash;
    @Column(name = "expires_at", nullable = false) public Instant expiresAt;
    @Column(name = "code_expires_at", nullable = false) public Instant codeExpiresAt;
    @Column(name = "code_failed_attempts", nullable = false) public int codeFailedAttempts;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
    @Column(name = "updated_at", nullable = false) public Instant updatedAt;
}
