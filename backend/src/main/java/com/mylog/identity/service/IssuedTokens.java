package com.mylog.identity.service;

import java.time.Instant;
import java.util.UUID;

public record IssuedTokens(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        UUID userId) {}
