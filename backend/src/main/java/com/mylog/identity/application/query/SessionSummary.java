package com.mylog.identity.application.query;

import java.time.Instant;
import java.util.UUID;

public record SessionSummary(UUID id, String deviceName, Instant createdAt, Instant lastUsedAt, Instant expiresAt) {}
