package com.mylog.safety.infrastructure.persistence;

import com.mylog.safety.application.SafetyPolicyGate;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JpaSafetyPolicyGate implements SafetyPolicyGate {
    private final EntityManager entityManager;

    JpaSafetyPolicyGate(EntityManager entityManager) { this.entityManager = entityManager; }

    @Override public Optional<String> approvedPolicy(String ruleVersion, String provider, String classifierVersion,
                                                      BigDecimal confidence, Instant at) {
        if (ruleVersion == null || provider == null || provider.isBlank() || classifierVersion == null
                || classifierVersion.isBlank() || confidence == null) return Optional.empty();
        return entityManager.createQuery("""
                select p from SafetyPolicyVersion p where p.status='APPROVED'
                  and p.approvedBy is not null and p.approvedAt is not null and p.approvedAt<=:at
                  and p.effectiveFrom is not null and p.effectiveFrom<=:at
                  and (p.effectiveTo is null or p.effectiveTo>:at)
                order by p.effectiveFrom desc
                """, SafetyPolicyVersion.class).setParameter("at", at).getResultList().stream()
                .filter(p -> approvedConfiguration(p, ruleVersion, provider, classifierVersion, confidence))
                .map(p -> p.version).findFirst();
    }

    private static boolean approvedConfiguration(SafetyPolicyVersion policy, String ruleVersion, String provider,
                                                 String classifierVersion, BigDecimal confidence) {
        var config = policy.config;
        if (config == null || !Boolean.TRUE.equals(config.get("allowOrdinaryAnalysis"))
                || !Objects.equals(ruleVersion, config.get("ruleVersion"))
                || !Objects.equals(provider, config.get("classifierProvider"))
                || !Objects.equals(classifierVersion, config.get("classifierVersion"))) return false;
        Object threshold = config.get("minConfidence");
        try {
            BigDecimal minimum = new BigDecimal(threshold.toString());
            return minimum.signum() >= 0 && minimum.compareTo(BigDecimal.ONE) <= 0
                    && confidence.compareTo(minimum) >= 0;
        } catch (RuntimeException e) { return false; }
    }
}
