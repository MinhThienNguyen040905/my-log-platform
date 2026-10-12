package com.mylog.safety.application;

import java.math.BigDecimal;
import java.util.Optional;

public interface RiskClassifier {
    Optional<Classification> classify(String plainText);

    default boolean requiresExternalConsent() { return false; }

    record Classification(String provider, String version, RiskLevel level, BigDecimal confidence) {}
}
