package com.mylog.knowledge.infrastructure.embedding;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "mylog.rag.embedding")
public record EmbeddingProperties(
        boolean enabled,
        String provider,
        String endpoint,
        String apiKey,
        String model,
        String modelVersion,
        @Min(0) @Max(2000) int dimensions,
        @Min(1) @Max(100) int batchSize,
        @Min(1000) @Max(120000) int timeoutMs,
        boolean providerDataApproved
) {
    @AssertTrue(message = "Gemini embedding configuration and provider data approval are required when RAG embedding is enabled")
    public boolean isEnabledConfigurationValid() {
        if (!enabled) return true;
        return "gemini".equals(provider)
                && hasText(endpoint) && hasText(apiKey) && hasText(model) && hasText(modelVersion)
                && (dimensions == 768 || dimensions == 1536)
                && providerDataApproved;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
