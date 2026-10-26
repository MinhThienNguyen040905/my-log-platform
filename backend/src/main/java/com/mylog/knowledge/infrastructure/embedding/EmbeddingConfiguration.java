package com.mylog.knowledge.infrastructure.embedding;

import com.mylog.knowledge.application.EmbeddingProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
class EmbeddingConfiguration {
    @Bean
    EmbeddingProvider embeddingProvider(EmbeddingProperties properties, ObjectMapper mapper) {
        if (!properties.enabled()) return new UnavailableEmbeddingProvider();
        if ("gemini".equals(properties.provider())) return new GeminiEmbeddingProvider(properties, mapper);
        throw new IllegalArgumentException("Unsupported embedding provider");
    }
}
