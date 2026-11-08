package com.mylog.safety.infrastructure;

import com.mylog.safety.application.RiskClassifier;
import com.mylog.safety.application.RiskLevel;
import com.mylog.safety.application.SafetyPolicyGate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * LOCAL DEVELOPMENT ONLY — stands in for the safety classifier while no trained model exists, so the
 * writing companion can be exercised end to end with synthetic data.
 * <ul>
 *   <li>The keyword rules in {@code SafetyScreeningService} (HIGH/CRITICAL) still run first, so crisis
 *       wording still routes to {@code SAFETY_FLOW}. Only the classifier step is replaced.</li>
 *   <li>Fails startup unless the {@code local} profile is active, so it can never reach staging/prod.</li>
 *   <li>Fails startup when {@code mylog.jobs.enabled=true}: {@code JpaSafetyAnalysisPermission} uses the same
 *       policy gate, so combining them would let saved journal entries flow to ordinary AI analysis.</li>
 *   <li>Only approves the {@value #PROVIDER} provider; real classifier providers keep needing a DB policy.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "mylog.safety.dev-allow", name = "enabled", havingValue = "true")
class DevSafetyAllowConfiguration {
    static final String PROVIDER = "dev-allow";
    static final String VERSION = "dev";
    private static final Logger log = LoggerFactory.getLogger(DevSafetyAllowConfiguration.class);

    DevSafetyAllowConfiguration(Environment env, @Value("${mylog.jobs.enabled:false}") boolean jobsEnabled) {
        if (!env.matchesProfiles("local")) {
            throw new IllegalStateException("MYLOG_SAFETY_DEV_ALLOW is only allowed with the local profile");
        }
        if (jobsEnabled) {
            throw new IllegalStateException("MYLOG_SAFETY_DEV_ALLOW cannot be combined with MYLOG_JOBS_ENABLED=true");
        }
        log.warn("DEV SAFETY ALLOW is active: the safety classifier is bypassed. Use synthetic data only.");
    }

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
    RiskClassifier devAllowRiskClassifier() {
        return text -> Optional.of(new RiskClassifier.Classification(PROVIDER, VERSION, RiskLevel.NORMAL, BigDecimal.ONE));
    }

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
    SafetyPolicyGate devAllowSafetyPolicyGate(@Qualifier("jpaSafetyPolicyGate") SafetyPolicyGate delegate) {
        return (ruleVersion, provider, classifierVersion, confidence, at) ->
                PROVIDER.equals(provider) && VERSION.equals(classifierVersion)
                        ? Optional.of("dev-allow-local")
                        : delegate.approvedPolicy(ruleVersion, provider, classifierVersion, confidence, at);
    }
}
