package com.mylog.user.api.response;

import com.mylog.user.application.query.ProfileView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProfileResponse(UUID userId, String displayName, String penName, List<String> onboardingGoals,
                              String timezone, String locale, Instant onboardingCompletedAt, long version,
                              boolean isAdmin) {
    public static ProfileResponse from(ProfileView view, boolean isAdmin) {
        return new ProfileResponse(view.userId(), view.displayName(), view.penName(), view.onboardingGoals(),
                view.timezone(), view.locale(), view.onboardingCompletedAt(), view.version(), isAdmin);
    }

    public static ProfileResponse from(ProfileView view) {
        return from(view, false);
    }
}
