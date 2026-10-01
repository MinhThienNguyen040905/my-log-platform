package com.mylog.identity.api.response;

import com.mylog.identity.application.result.AuthResult;

import java.util.UUID;

public record AuthResponse(UUID userId, String accessToken, String refreshToken, long expiresInSeconds) {
    public static AuthResponse from(AuthResult result) {
        return new AuthResponse(result.userId(), result.accessToken(), result.refreshToken(), result.expiresInSeconds());
    }
}
