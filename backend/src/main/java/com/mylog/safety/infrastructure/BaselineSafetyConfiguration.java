package com.mylog.safety.infrastructure;

import com.mylog.safety.application.RiskClassifier;
import com.mylog.safety.application.RiskLevel;
import com.mylog.safety.application.SafetyPolicyGate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Pattern;

/** Rule baseline for the MVP; not a trained or evaluated safety classifier. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "mylog.ai", name = "mvp-enabled", havingValue = "true")
class BaselineSafetyConfiguration {
    private static final String PROVIDER = "mylog-mvp-rules";
    private static final String VERSION = "v1";
    private static final String RULE_VERSION = "m2-curated-draft-v1";
    private static final String POLICY_VERSION = "mvp-rules-v1";
    private static final Pattern CAUTION = Pattern.compile(
            "(?i)\\b(done with all this|can't go on|cannot go on|hopeless|worthless|hurt myself)\\b"
                    + "|không còn chịu nổi|hết hy vọng|muốn biến mất|tự làm đau");

    @Bean
    @Primary
    RiskClassifier mvpRiskClassifier() {
        return text -> Optional.of(new RiskClassifier.Classification(PROVIDER, VERSION,
                CAUTION.matcher(text).find() ? RiskLevel.MODERATE : RiskLevel.LOW,
                new BigDecimal("0.5")));
    }

    @Bean
    @Primary
    SafetyPolicyGate mvpSafetyPolicyGate() {
        return (rule, provider, version, confidence, at) ->
                RULE_VERSION.equals(rule) && PROVIDER.equals(provider) && VERSION.equals(version)
                        && confidence != null && confidence.compareTo(new BigDecimal("0.5")) >= 0
                        ? Optional.of(POLICY_VERSION) : Optional.empty();
    }
}
