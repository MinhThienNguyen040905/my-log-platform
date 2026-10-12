package com.mylog.safety.infrastructure;

import com.mylog.safety.application.RiskClassifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "mylog.ai", name = "mvp-enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "mylog.safety.classifier", name = "provider", havingValue = "groq")
class GroqSafetyConfiguration {
    @Bean
    @Primary
    RiskClassifier groqRiskClassifier(@Value("${mylog.ai.provider:none}") String provider,
                                      @Value("${mylog.ai.endpoint:}") String endpoint,
                                      @Value("${mylog.ai.model:}") String model,
                                      @Value("${mylog.ai.api-key:}") String apiKey,
                                      @Value("${mylog.safety.classifier.timeout-ms:10000}") int timeoutMs,
                                      ObjectMapper mapper) {
        if (!"groq".equals(provider)) throw new IllegalArgumentException("Groq safety requires Groq AI provider");
        String resolvedEndpoint = endpoint.isBlank()
                ? "https://api.groq.com/openai/v1/chat/completions" : endpoint;
        return new GroqRiskClassifier(resolvedEndpoint, model, apiKey, timeoutMs, mapper);
    }
}
