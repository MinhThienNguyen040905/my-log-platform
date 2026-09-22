package com.mylog.identity.service;

import com.mylog.identity.entity.UserPlan;
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
