package com.mylog.identity.application;

import java.time.Instant;
import java.util.UUID;

public record PasswordResetRequestedEvent(UUID userId, String email, String token, String code, Instant requestedAt) {}

