package com.mylog.safety.infrastructure;

import com.mylog.safety.application.RiskClassifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class RiskClassifierConfiguration {
    @Bean
    @ConditionalOnMissingBean(RiskClassifier.class)
    RiskClassifier unavailableRiskClassifier() { return new UnavailableRiskClassifier(); }
}
