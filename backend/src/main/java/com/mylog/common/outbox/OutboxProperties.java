package com.mylog.common.outbox;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "mylog.outbox")
public record OutboxProperties(
        boolean schedulingEnabled,
        @Positive int batchSize,
        @Positive int maxAttempts,
        @NotNull Duration pollInterval,
        @NotNull Duration claimLease,
        @NotNull Duration confirmTimeout,
        @NotNull Duration retryBaseDelay,
        @NotNull Duration retryMaxDelay) {}
