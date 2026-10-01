package com.mylog.safety.infrastructure.persistence;

import com.mylog.safety.application.SafetyAnalysisPermission;
import com.mylog.safety.application.SafetyPolicyGate;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.util.UUID;

@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JpaSafetyAnalysisPermission implements SafetyAnalysisPermission {
    private final EntityManager em;
    private final SafetyPolicyGate policies;
    private final Clock clock;

    JpaSafetyAnalysisPermission(EntityManager em, SafetyPolicyGate policies, Clock clock) {
        this.em = em; this.policies = policies; this.clock = clock;
    }

    @Override public boolean permits(UUID userId, UUID entryId) {
        SafetyEvent event = latest(userId, entryId);
        return event != null && ("ALLOW".equals(event.decision) || "CONSTRAIN".equals(event.decision))
                && policies.approvedPolicy(event.ruleVersion, event.classifier, event.classifierVersion,
                        event.confidence, clock.instant()).filter(event.policyVersion::equals).isPresent();
    }

    @Override public String policyVersion(UUID userId, UUID entryId) {
        SafetyEvent event = latest(userId, entryId);
        return event == null ? null : event.policyVersion;
    }

    private SafetyEvent latest(UUID userId, UUID entryId) {
        return em.createQuery("""
                select e from SafetyEvent e where e.userId=:userId and e.journalEntryId=:entryId
                order by e.createdAt desc, e.id desc
                """, SafetyEvent.class).setParameter("userId", userId).setParameter("entryId", entryId)
                .setMaxResults(1).getResultStream().findFirst().orElse(null);
    }
}
