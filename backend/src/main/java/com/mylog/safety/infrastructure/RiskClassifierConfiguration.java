package com.mylog.safety.infrastructure;

import com.mylog.safety.application.RiskClassifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class RiskClassifierConfiguration {
    @Bean
    @ConditionalOnMissingBean(RiskClassifier.class)
    RiskClassifier riskClassifier(@Value("${mylog.safety.classifier.url:}") String url,
                                  @Value("${mylog.safety.classifier.token:}") String token,
                                  @Value("${mylog.safety.classifier.timeout-ms:2000}") int timeoutMs) {
        if (url.isBlank()) return new UnavailableRiskClassifier();
        return new HttpRiskClassifier(url, token, timeoutMs);
    }
}
