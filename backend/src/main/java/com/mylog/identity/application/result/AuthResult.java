package com.mylog.identity.application.result;

import java.util.UUID;

/** {@code deletionCancelled}: this sign-in cancelled a pending account deletion (UC-23). */
public record AuthResult(UUID userId, String accessToken, String refreshToken, long expiresInSeconds,
                         boolean deletionCancelled) {
    public AuthResult(UUID userId, String accessToken, String refreshToken, long expiresInSeconds) {
        this(userId, accessToken, refreshToken, expiresInSeconds, false);
    }
}
