package com.mylog.user.api.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateProfileRequest(@Size(max = 120) String displayName,
                                   @Size(max = 120) String penName,
                                   List<@Pattern(regexp = "[A-Z_]+") String> onboardingGoals,
                                   String timezone,
                                   String locale) {}
