package com.mylog.shared.security;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Base64;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "mylog.security.jwt")
public record JwtProperties(
        @NotBlank String issuer,
        @NotNull Duration accessTokenTtl,
        @NotNull Duration refreshTokenTtl,
        @NotBlank String signingKeyBase64) {

    private static final int MINIMUM_KEY_BYTES = 32;

    @AssertTrue(message = "JWT signing key must be Base64URL encoded and contain at least 32 bytes")
    public boolean isSigningKeyStrongEnough() {
        if (signingKeyBase64 == null || signingKeyBase64.isBlank()) {
            return false;
        }

        try {
            return Base64.getUrlDecoder().decode(signingKeyBase64).length >= MINIMUM_KEY_BYTES;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public byte[] decodedSigningKey() {
        return Base64.getUrlDecoder().decode(signingKeyBase64);
    }
}
