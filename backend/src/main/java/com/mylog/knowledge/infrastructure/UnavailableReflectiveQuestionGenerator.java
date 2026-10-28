package com.mylog.knowledge.infrastructure;

import com.mylog.analysis.application.KnowledgeRetriever;
import com.mylog.knowledge.application.ReflectiveQuestionGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration(proxyBeanMethods = false)
class UnavailableReflectiveQuestionGenerator {
    @Bean
    @ConditionalOnMissingBean(ReflectiveQuestionGenerator.class)
    ReflectiveQuestionGenerator unavailableReflectiveQuestionGenerator() {
        return (topicCode, excerpts) -> {
            throw new IllegalStateException("Reflective question provider is not configured");
        };
    }
}
