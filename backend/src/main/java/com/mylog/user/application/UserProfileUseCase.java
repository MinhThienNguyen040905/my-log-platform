package com.mylog.user.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface UserProfileUseCase {
    record ProfileView(UUID userId, String displayName, String penName, List<String> onboardingGoals, String timezone,
                       String locale, Instant onboardingCompletedAt, long version) {}
    record ConsentView(String type, String documentVersion, boolean granted, Instant decidedAt) {}

    void createDefault(UUID userId, String timezone, String locale, String termsVersion,
                       String privacyVersion, Instant now);
    ProfileView get(UUID userId);
    ProfileView update(UUID userId, String displayName, String penName, List<String> onboardingGoals, String timezone,
                       String locale, long expectedVersion);
    ProfileView completeOnboarding(UUID userId, long expectedVersion);
    void decideConsent(UUID userId, String type, String documentVersion, boolean granted);
    List<ConsentView> consents(UUID userId);
    boolean isConsentGranted(UUID userId, String type);
}
