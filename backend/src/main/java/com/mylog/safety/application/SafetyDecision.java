package com.mylog.safety.application;

import java.math.BigDecimal;

public record SafetyDecision(RiskLevel riskLevel, String decision, String ruleVersion,
                             String classifier, String classifierVersion, String policyVersion,
                             BigDecimal confidence, String resourceSetVersion) {
    public boolean permitsOrdinaryAnalysis() {
        return "ALLOW".equals(decision);
    }
}
