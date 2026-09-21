package com.mylog.identity.application;

import java.time.Instant;
import java.util.UUID;

public record IssuedTokens(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        UUID userId) {}
