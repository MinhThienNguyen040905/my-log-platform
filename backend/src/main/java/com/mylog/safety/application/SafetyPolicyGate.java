package com.mylog.safety.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/** An explicit approval is required before ordinary analysis may be queued. */
public interface SafetyPolicyGate {
    Optional<String> approvedPolicy(String ruleVersion, String classifierProvider,
                                    String classifierVersion, BigDecimal confidence, Instant at);
}
