package com.mylog.user.application.query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProfileView(UUID userId, String displayName, String penName, List<String> onboardingGoals,
                          String timezone, String locale, Instant onboardingCompletedAt, long version) {}
