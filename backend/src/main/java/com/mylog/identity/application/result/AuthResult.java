package com.mylog.identity.application.result;

import java.util.UUID;

public record AuthResult(UUID userId, String accessToken, String refreshToken, long expiresInSeconds) {}
