package com.mylog.identity.application;

import java.time.Instant;
import java.util.UUID;

public record PasswordChangedEvent(UUID userId, String email, Instant changedAt, String remoteAddress) {}

