package com.mylog.identity.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "mylog.security.password")
public record PasswordProperties(@Min(4) @Max(16) int bcryptStrength) {}
