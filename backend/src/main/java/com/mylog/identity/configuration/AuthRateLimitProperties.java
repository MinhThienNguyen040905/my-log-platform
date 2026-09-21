package com.mylog.identity.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "mylog.security.rate-limit")
public record AuthRateLimitProperties(
        @NotBlank String keyPrefix,
        @NotNull Duration window,
        @Positive int registerAttempts,
        @Positive int loginAttempts,
        @Positive int refreshAttempts) {}
