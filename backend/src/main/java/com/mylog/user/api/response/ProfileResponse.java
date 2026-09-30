package com.mylog.user.api.response;

import com.mylog.user.application.query.ProfileView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProfileResponse(UUID userId, String displayName, String penName, List<String> onboardingGoals,
                              String timezone, String locale, Instant onboardingCompletedAt, long version) {
    public static ProfileResponse from(ProfileView view) {
        return new ProfileResponse(view.userId(), view.displayName(), view.penName(), view.onboardingGoals(),
                view.timezone(), view.locale(), view.onboardingCompletedAt(), view.version());
    }
}
