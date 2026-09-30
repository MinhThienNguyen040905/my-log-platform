package com.mylog.platform.storage;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "mylog.cloudinary")
public record CloudinaryProperties(
        boolean enabled,
        String cloudName,
        String apiKey,
        String apiSecret,
        @NotBlank String folder
) {
    @AssertTrue(message = "Cloudinary credentials are required when mylog.cloudinary.enabled=true")
    public boolean isCredentialsConfigured() {
        return !enabled || hasText(cloudName) && hasText(apiKey) && hasText(apiSecret);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
