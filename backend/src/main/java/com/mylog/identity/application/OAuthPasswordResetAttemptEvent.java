package com.mylog.identity.application;

import java.time.Instant;
import java.util.UUID;

public record OAuthPasswordResetAttemptEvent(UUID userId, String email, Instant requestedAt) {}

