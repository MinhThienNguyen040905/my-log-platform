package com.mylog.safety.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;
import java.time.Clock;
import java.util.regex.Pattern;

@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public final class SafetyScreeningService implements SafetyScreeningUseCase {
    private static final String RULE_VERSION = "m2-curated-draft-v1";
    private static final String POLICY_VERSION = "m2-fail-safe-v1";
    private static final Pattern CRITICAL = Pattern.compile(
            "\\b(kill myself now|end my life now|suicide tonight)\\b|(?:tự tử|tự sát).{0,40}(?:ngay|tối nay|hôm nay)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern HIGH = Pattern.compile(
            "\\b(kill myself|end my life|want to die|self harm)\\b|(?:muốn chết|không muốn sống|tự tử|tự sát|tự hại)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private final RiskClassifier classifier;
    private final SafetyPolicyGate policyGate;
    private final Clock clock;

    public SafetyScreeningService(RiskClassifier classifier, SafetyPolicyGate policyGate, Clock clock) {
        this.classifier = classifier;
        this.policyGate = policyGate;
        this.clock = clock;
    }

    @Override public SafetyDecision screen(String plainText) {
        String normalized = plainText.toLowerCase(Locale.ROOT);
        if (CRITICAL.matcher(normalized).find()) return ruleDecision(RiskLevel.CRITICAL);
        if (HIGH.matcher(normalized).find()) return ruleDecision(RiskLevel.HIGH);
        try {
            Optional<RiskClassifier.Classification> classified = classifier.classify(plainText);
            if (classified.isEmpty()) return failSafe();
            RiskClassifier.Classification result = classified.get();
            if (result.level() == null || result.confidence() == null || result.confidence().signum() < 0
                    || result.confidence().compareTo(java.math.BigDecimal.ONE) > 0) return failSafe();
            if (result.level() == RiskLevel.HIGH || result.level() == RiskLevel.CRITICAL)
                return new SafetyDecision(result.level(), "SAFETY_FLOW", RULE_VERSION, result.provider(),
                        result.version(), POLICY_VERSION, result.confidence(), null);
            Optional<String> approved = policyGate.approvedPolicy(RULE_VERSION, result.provider(),
                    result.version(), result.confidence(), clock.instant());
            if (approved.isEmpty()) return failSafe();
            String decision = switch (result.level()) {
                case MODERATE -> "CONSTRAIN";
                case NORMAL, LOW -> "ALLOW";
                case UNKNOWN -> "FAIL_SAFE";
                case HIGH, CRITICAL -> throw new IllegalStateException("Handled before policy lookup");
            };
            return new SafetyDecision(result.level(), decision, RULE_VERSION, result.provider(), result.version(),
                    approved.get(), result.confidence(), null);
        } catch (RuntimeException ignored) {
            return failSafe();
        }
    }

    private static SafetyDecision ruleDecision(RiskLevel level) {
        return new SafetyDecision(level, "SAFETY_FLOW", RULE_VERSION, null, null, POLICY_VERSION, null, null);
    }

    private static SafetyDecision failSafe() {
        return new SafetyDecision(RiskLevel.UNKNOWN, "FAIL_SAFE", RULE_VERSION, null, null, POLICY_VERSION, null, null);
    }
}
