package com.mylog.analysis.infrastructure;

import com.mylog.analysis.application.PatternExtractor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

/**
 * Thinking-pattern extractor ({@code mylog.patterns.extractor}): {@code none} (default, no bean), {@code fake}
 * (keyword rules, local/test profiles only) or {@code openai} (prototype, {@code local} profile only, with its
 * own {@code mylog.patterns.ai.*} settings so enabling it never implies the journal-analysis provider approval).
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnExpression("'${mylog.identity.enabled:false}' == 'true' and '${mylog.patterns.extractor:none}' != 'none'")
class PatternExtractorConfiguration {
    @Bean
    PatternExtractor patternExtractor(Environment env,
                                      @Value("${mylog.patterns.extractor}") String extractor,
                                      @Value("${mylog.patterns.ai.endpoint:https://api.openai.com/v1/chat/completions}") String endpoint,
                                      @Value("${mylog.patterns.ai.model:}") String model,
                                      @Value("${mylog.patterns.ai.api-key:}") String apiKey,
                                      @Value("${mylog.patterns.ai.timeout-ms:15000}") int timeoutMs,
                                      @Value("${mylog.patterns.ai.input-usd-per-million:0}") BigDecimal inputPrice,
                                      @Value("${mylog.patterns.ai.output-usd-per-million:0}") BigDecimal outputPrice,
                                      ObjectMapper mapper) {
        boolean production = env.matchesProfiles("staging | prod");
        return switch (extractor) {
            case "fake" -> {
                if (production || !env.matchesProfiles("local | test"))
                    throw new IllegalStateException("The fake thinking-pattern extractor is for local/test profiles only");
                yield new FakePatternExtractor();
            }
            case "openai" -> {
                if (production || !env.matchesProfiles("local"))
                    throw new IllegalStateException("The OpenAI thinking-pattern extractor is a local prototype with synthetic data only");
                yield new OpenAiPatternExtractor(endpoint, model, apiKey, timeoutMs, mapper, inputPrice, outputPrice);
            }
            default -> throw new IllegalArgumentException("Unsupported thinking-pattern extractor");
        };
    }
}
