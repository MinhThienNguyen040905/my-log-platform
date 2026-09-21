package com.mylog.identity.application;

import com.mylog.identity.domain.UserPlan;
import java.time.Instant;
import java.util.UUID;

public record UserProfile(
        UUID id,
        String email,
        String displayName,
        UserPlan plan,
        String timezone,
        String language,
        Instant createdAt,
        Instant updatedAt) {

    @Override
    public String toString() {
        return "UserProfile[id=" + id + "]";
    }
}
