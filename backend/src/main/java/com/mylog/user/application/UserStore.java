package com.mylog.user.application;

import com.mylog.platform.crypto.SensitiveDataCipher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserStore {
    record Profile(UUID userId, SensitiveDataCipher.Encrypted payload, String timezone, String locale,
                   Instant onboardingCompletedAt, long version) {}
    record Consent(String type, String documentVersion, boolean granted, Instant decidedAt) {}

    void create(Profile profile, Instant now);
    Optional<Profile> find(UUID userId);
    boolean update(Profile profile, long expectedVersion, Instant now);
    boolean completeOnboarding(UUID userId, long expectedVersion, Instant now);
    void addConsent(UUID id, UUID userId, String type, String documentVersion,
                    boolean granted, String source, Instant now);
    List<Consent> latestConsents(UUID userId);
}
