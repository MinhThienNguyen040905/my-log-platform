package com.mylog.journal.infrastructure;

import com.mylog.journal.application.WritingSuggestionGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class WritingSuggestionGeneratorConfiguration {
    @Bean
    WritingSuggestionGenerator writingSuggestionGenerator(
            @Value("${mylog.writing-suggestions.ai-enabled:false}") boolean aiEnabled,
            @Value("${mylog.ai.provider:none}") String provider,
            @Value("${mylog.ai.endpoint:}") String endpoint,
            @Value("${mylog.ai.model:}") String model,
            @Value("${mylog.ai.api-key:}") String apiKey,
            @Value("${mylog.ai.timeout-ms:30000}") int timeoutMs,
            @Value("${mylog.ai.provider-data-approved:false}") boolean providerDataApproved,
            @Value("${mylog.ai.output-safety-approved:false}") boolean outputSafetyApproved,
            @Value("${mylog.ai.mvp-enabled:false}") boolean mvpEnabled,
            ObjectMapper mapper) {
        if (!aiEnabled) return new TemplateWritingSuggestionGenerator();
        if (!"groq".equals(provider) || !mvpEnabled && (!providerDataApproved || !outputSafetyApproved))
            throw new IllegalArgumentException("Groq writing suggestions require provider and safety approval");
        String resolvedEndpoint = endpoint.isBlank()
                ? "https://api.groq.com/openai/v1/chat/completions" : endpoint;
        return new GroqWritingSuggestionGenerator(resolvedEndpoint, model, apiKey, timeoutMs, mapper);
    }
}
