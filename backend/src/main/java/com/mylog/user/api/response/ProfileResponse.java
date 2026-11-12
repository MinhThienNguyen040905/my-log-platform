package com.mylog.user.api.response;

import com.mylog.user.application.query.ProfileView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProfileResponse(UUID userId, String displayName, String penName, List<String> onboardingGoals,
                              String timezone, String locale, Instant onboardingCompletedAt, long version,
                              boolean isAdmin, boolean canChangePassword, String mascot, String email) {
    /** {@code email} is the account's own address, decrypted for its owner; null when not available. */
    public static ProfileResponse from(ProfileView view, boolean isAdmin, boolean canChangePassword, String email) {
        return new ProfileResponse(view.userId(), view.displayName(), view.penName(), view.onboardingGoals(),
                view.timezone(), view.locale(), view.onboardingCompletedAt(), view.version(), isAdmin, canChangePassword,
                view.mascot(), email);
    }
}
