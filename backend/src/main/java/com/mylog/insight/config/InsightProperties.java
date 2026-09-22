package com.mylog.insight.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "mylog.insight")
public record InsightProperties(boolean consumerEnabled, @Min(7) @Max(365) int lookbackDays) {}
