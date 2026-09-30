package com.mylog.safety.infrastructure.persistence;

import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.platform.id.IdGenerator;
import com.mylog.safety.application.SafetyDecision;
import com.mylog.safety.application.SafetyEventRecorder;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JpaSafetyEventRecorder implements SafetyEventRecorder {
    private final EntityManager entityManager;
    private final IdGenerator ids;
    private final SensitiveDataCipher cipher;

    JpaSafetyEventRecorder(EntityManager entityManager, IdGenerator ids, SensitiveDataCipher cipher) {
        this.entityManager = entityManager;
        this.ids = ids;
        this.cipher = cipher;
    }

    @Override public void record(UUID userId, UUID entryId, SafetyDecision decision, Instant now) {
        SafetyEvent event = new SafetyEvent();
        event.id = ids.next();
        event.userId = userId;
        event.subjectRef = cipher.lookupHash("safety-subject:" + userId);
        event.journalEntryId = entryId;
        event.riskLevel = decision.riskLevel().name();
        event.decision = decision.decision();
        event.ruleVersion = decision.ruleVersion();
        event.classifier = decision.classifier();
        event.classifierVersion = decision.classifierVersion();
        event.policyVersion = decision.policyVersion();
        event.confidence = decision.confidence();
        event.resourceSetVersion = decision.resourceSetVersion();
        event.createdAt = now;
        entityManager.persist(event);
    }
}
