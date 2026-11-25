package com.mylog.user.application;

import com.mylog.user.application.query.ConsentView;
import com.mylog.user.application.query.ProfileView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface UserProfileUseCase {
    void validateRegistrationPreferences(String timezone, String locale);
    void createDefault(UUID userId, String timezone, String locale, String termsVersion,
                       String privacyVersion, Instant now);
    ProfileView get(UUID userId);
    ProfileView update(UUID userId, String displayName, String penName, List<String> onboardingGoals, String timezone,
                       String locale, String mascot, long expectedVersion);
    ProfileView completeOnboarding(UUID userId, long expectedVersion);
    /**
     * "Start over": clears display name, pen name, onboarding goals and mascot; keeps timezone, locale and the
     * onboarding completion so the user does not repeat onboarding.
     */
    void resetPersonalDetails(UUID userId);
    void decideConsent(UUID userId, String type, String documentVersion, boolean granted);
    List<ConsentView> consents(UUID userId);
    boolean isConsentGranted(UUID userId, String type);
}
