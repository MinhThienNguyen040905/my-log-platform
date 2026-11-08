package com.mylog.journal.infrastructure;

import com.mylog.journal.application.CompanionResponder;
import com.mylog.safety.application.SafetyScreeningUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "mylog.companion.generation", name = "enabled", havingValue = "true")
class CompanionResponderConfiguration {
    @Bean
    @Primary
    CompanionResponder llmCompanionResponder(
            @Value("${mylog.ai.provider:none}") String provider,
            @Value("${mylog.ai.endpoint:https://api.openai.com/v1/chat/completions}") String endpoint,
            @Value("${mylog.ai.model:}") String model,
            @Value("${mylog.ai.api-key:}") String apiKey,
            @Value("${mylog.companion.generation.timeout-ms:4000}") int timeoutMs,
            @Value("${mylog.ai.provider-data-approved:false}") boolean providerDataApproved,
            @Value("${mylog.ai.output-safety-approved:false}") boolean outputSafetyApproved,
            @Value("${mylog.companion.generation.outbound-safety-hard-block:true}") boolean outboundSafetyHardBlock,
            ObjectMapper mapper,
            TemplateCompanionResponder templateFallback,
            SafetyScreeningUseCase outputScreening) {
        if (!"openai".equals(provider)) {
            throw new IllegalArgumentException("Companion generation requires mylog.ai.provider=openai");
        }
        if (!providerDataApproved || !outputSafetyApproved) {
            throw new IllegalArgumentException("AI provider and output safety approval required for companion generation");
        }
        return new LlmCompanionResponder(endpoint, model, apiKey, timeoutMs, mapper,
                templateFallback, outputScreening, outboundSafetyHardBlock);
    }
}
