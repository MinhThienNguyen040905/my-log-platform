package com.mylog.user.api.response;

import com.mylog.identity.application.query.SessionSummary;

import java.time.Instant;
import java.util.UUID;

/** {@code current} marks the session the request was made with (the JWT {@code sid} claim). */
public record SessionResponse(UUID id, String deviceName, Instant createdAt, Instant lastUsedAt, Instant expiresAt,
                              boolean current) {
    public static SessionResponse from(SessionSummary summary, UUID currentSession) {
        return new SessionResponse(summary.id(), summary.deviceName(), summary.createdAt(), summary.lastUsedAt(),
                summary.expiresAt(), summary.id().equals(currentSession));
    }
}
