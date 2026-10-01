package com.mylog.user.api.response;

import com.mylog.identity.application.query.SessionSummary;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(UUID id, String deviceName, Instant createdAt, Instant lastUsedAt, Instant expiresAt) {
    public static SessionResponse from(SessionSummary summary) {
        return new SessionResponse(summary.id(), summary.deviceName(), summary.createdAt(), summary.lastUsedAt(),
                summary.expiresAt());
    }
}
