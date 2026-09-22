package com.mylog.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SensitiveDataSanitizerTest {

    private final SensitiveDataSanitizer sanitizer = new SensitiveDataSanitizer();

    @Test
    void redactsCommonSecretFormats() {
        String input = "Authorization: Bearer jwt-value password=my-secret api_key=provider-key";

        String sanitized = sanitizer.sanitize(input);

        assertThat(sanitized)
                .doesNotContain("jwt-value", "my-secret", "provider-key")
                .contains("[REDACTED]");
    }

    @Test
    void preservesOrdinaryMessages() {
        assertThat(sanitizer.sanitize("Database connection timed out"))
                .isEqualTo("Database connection timed out");
    }
}
