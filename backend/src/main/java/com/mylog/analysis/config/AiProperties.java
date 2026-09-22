package com.mylog.analysis.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "mylog.ai")
public record AiProperties(
        @NotBlank String provider,
        String apiKey,
        @NotNull URI endpoint,
        @NotBlank String model,
        @NotBlank String promptVersion,
        @NotBlank String schemaVersion,
        boolean consumerEnabled,
        boolean schedulingEnabled,
        @Positive int batchSize,
        @Positive int maxAttempts,
        @NotNull Duration pollInterval,
        @NotNull Duration claimLease,
        @NotNull Duration connectTimeout,
        @NotNull Duration responseTimeout,
        @NotNull Duration retryBaseDelay,
        @NotNull Duration retryMaxDelay,
        @Positive int circuitFailureThreshold,
        @NotNull Duration circuitOpenDuration,
        @Min(1) @Max(10) int maxReflections) {}
