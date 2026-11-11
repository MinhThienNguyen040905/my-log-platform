package com.mylog.insight.infrastructure;

import com.mylog.insight.application.InsightNarrator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

/** AI wording for pattern insights; off unless explicitly enabled and the provider is approved. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "mylog.insights.ai-narrative", name = "enabled", havingValue = "true")
@ConditionalOnBooleanProperty("mylog.identity.enabled")
class InsightNarratorConfiguration {
    @Bean
    InsightNarrator llmInsightNarrator(
            @Value("${mylog.ai.provider:none}") String provider,
            @Value("${mylog.ai.endpoint:https://api.openai.com/v1/chat/completions}") String endpoint,
            @Value("${mylog.ai.model:}") String model,
            @Value("${mylog.ai.api-key:}") String apiKey,
            @Value("${mylog.insights.ai-narrative.timeout-ms:10000}") int timeoutMs,
            @Value("${mylog.ai.input-usd-per-million:0}") BigDecimal inputUsdPerMillion,
            @Value("${mylog.ai.output-usd-per-million:0}") BigDecimal outputUsdPerMillion,
            @Value("${mylog.ai.provider-data-approved:false}") boolean providerDataApproved,
            @Value("${mylog.ai.output-safety-approved:false}") boolean outputSafetyApproved,
            ObjectMapper mapper) {
        if (!"openai".equals(provider)) {
            throw new IllegalArgumentException("Insight AI narrative requires mylog.ai.provider=openai");
        }
        if (!providerDataApproved || !outputSafetyApproved) {
            throw new IllegalArgumentException("AI provider and output safety approval required for insight AI narrative");
        }
        return new LlmInsightNarrator(endpoint, model, apiKey, timeoutMs, mapper, inputUsdPerMillion, outputUsdPerMillion);
    }
}
