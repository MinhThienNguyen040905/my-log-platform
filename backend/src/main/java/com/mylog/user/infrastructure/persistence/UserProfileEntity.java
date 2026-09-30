package com.mylog.user.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
class UserProfileEntity {
    @Id @Column(name = "user_id") UUID userId;
    @Column(name = "encrypted_profile", nullable = false) byte[] encryptedProfile;
    @Column(name = "profile_iv", nullable = false) byte[] profileIv;
    @Column(name = "profile_wrapped_key", nullable = false) byte[] profileWrappedKey;
    @Column(name = "profile_key_version", nullable = false) String profileKeyVersion;
    @Column(name = "timezone", nullable = false) String timezone;
    @Column(name = "locale", nullable = false) String locale;
    @Column(name = "onboarding_completed_at") Instant onboardingCompletedAt;
    @Column(name = "preferred_journal_time") LocalTime preferredJournalTime;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;
    @Column(name = "row_version", nullable = false) long rowVersion;
}
