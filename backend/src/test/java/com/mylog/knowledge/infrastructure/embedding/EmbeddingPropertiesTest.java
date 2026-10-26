package com.mylog.knowledge.infrastructure.embedding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddingPropertiesTest {
    @Test void disabledConfigurationDoesNotRequireCredentials() {
        assertTrue(properties(false, "", false, 0).isEnabledConfigurationValid());
    }

    @Test void enabledGeminiRequiresCredentialsApprovalAndSupportedDimensions() {
        assertTrue(properties(true, "test-key", true, 768).isEnabledConfigurationValid());
        assertTrue(properties(true, "test-key", true, 1536).isEnabledConfigurationValid());
        assertFalse(properties(true, "", true, 768).isEnabledConfigurationValid());
        assertFalse(properties(true, "test-key", false, 768).isEnabledConfigurationValid());
        assertFalse(properties(true, "test-key", true, 1024).isEnabledConfigurationValid());
    }

    private static EmbeddingProperties properties(boolean enabled, String key, boolean approved, int dimensions) {
        return new EmbeddingProperties(enabled, "gemini",
                "https://generativelanguage.googleapis.com/v1beta", key,
                "gemini-embedding-001", "gemini-embedding-001", dimensions, 32, 30000, approved);
    }
}
