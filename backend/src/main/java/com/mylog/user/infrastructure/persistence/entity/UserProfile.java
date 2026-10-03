package com.mylog.user.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
public class UserProfile {
    @Id @Column(name = "user_id") public UUID userId;
    @Column(name = "encrypted_profile", nullable = false) public byte[] encryptedProfile;
    @Column(name = "profile_iv", nullable = false) public byte[] profileIv;
    @Column(name = "profile_wrapped_key", nullable = false) public byte[] profileWrappedKey;
    @Column(name = "profile_key_version", nullable = false) public String profileKeyVersion;
    @Column(name = "timezone", nullable = false) public String timezone;
    @Column(name = "locale", nullable = false) public String locale;
    @Column(name = "onboarding_completed_at") public Instant onboardingCompletedAt;
    @Column(name = "preferred_journal_time") public LocalTime preferredJournalTime;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
    @Column(name = "updated_at", nullable = false) public Instant updatedAt;
    @Column(name = "row_version", nullable = false) public long rowVersion;
}
