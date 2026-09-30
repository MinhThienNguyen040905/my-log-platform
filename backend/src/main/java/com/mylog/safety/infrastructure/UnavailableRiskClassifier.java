package com.mylog.safety.infrastructure;

import com.mylog.safety.application.RiskClassifier;

import java.util.Optional;

final class UnavailableRiskClassifier implements RiskClassifier {
    @Override public Optional<Classification> classify(String plainText) { return Optional.empty(); }
}
