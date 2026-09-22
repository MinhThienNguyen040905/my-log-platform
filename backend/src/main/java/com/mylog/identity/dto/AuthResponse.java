package com.mylog.identity.dto;

import com.mylog.identity.service.AuthenticationResult;
import com.mylog.identity.service.UserProfile;
import java.time.Clock;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserProfile user) {

    public static AuthResponse from(AuthenticationResult result, Clock clock) {
        long expiresIn = Math.max(
                0,
                result.tokens().accessTokenExpiresAt().getEpochSecond()
                        - clock.instant().getEpochSecond());
        return new AuthResponse(result.tokens().accessToken(), "Bearer", expiresIn, result.user());
    }

    @Override
    public String toString() {
        return "AuthResponse[accessToken=REDACTED,userId=" + user.id() + "]";
    }
}
