package com.mylog.safety.application;

import java.math.BigDecimal;
import java.util.Optional;

public interface RiskClassifier {
    Optional<Classification> classify(String plainText);

    record Classification(String provider, String version, RiskLevel level, BigDecimal confidence) {}
}
