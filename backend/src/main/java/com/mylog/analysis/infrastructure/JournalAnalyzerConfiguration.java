package com.mylog.analysis.infrastructure;

import com.mylog.analysis.application.JournalAnalyzer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "mylog.jobs", name = "enabled", havingValue = "true")
class JournalAnalyzerConfiguration {
    @Bean
    JournalAnalyzer journalAnalyzer(@Value("${mylog.ai.fake-enabled:false}") boolean fake,
                                    @Value("${mylog.ai.provider:none}") String provider,
                                    @Value("${mylog.ai.endpoint:https://api.openai.com/v1/chat/completions}") String endpoint,
                                    @Value("${mylog.ai.model:}") String model,
                                    @Value("${mylog.ai.api-key:}") String apiKey,
                                    @Value("${mylog.ai.timeout-ms:30000}") int timeoutMs,
                                    @Value("${mylog.ai.input-usd-per-million:0}") java.math.BigDecimal inputPrice,
                                    @Value("${mylog.ai.output-usd-per-million:0}") java.math.BigDecimal outputPrice,
                                    @Value("${mylog.ai.provider-data-approved:false}") boolean providerDataApproved,
                                    @Value("${mylog.ai.output-safety-approved:false}") boolean outputSafetyApproved,
                                    ObjectMapper mapper) {
        if (fake && !"none".equals(provider))
            throw new IllegalArgumentException("Choose either fake or configured AI provider");
        if (fake) return new FakeJournalAnalyzer();
        if ("none".equals(provider)) return new UnavailableJournalAnalyzer();
        if ("openai".equals(provider)) {
            if (!providerDataApproved || !outputSafetyApproved)
                throw new IllegalArgumentException("AI provider and output safety approval required");
            return new OpenAiJournalAnalyzer(endpoint, model, apiKey, timeoutMs,
                    inputPrice, outputPrice, mapper);
        }
        throw new IllegalArgumentException("Unsupported AI provider");
    }
}
