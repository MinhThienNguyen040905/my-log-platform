package com.mylog.identity.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "mylog.security.policy")
public record PolicyProperties(
        @NotBlank String termsVersion,
        @NotBlank String privacyVersion) {}
