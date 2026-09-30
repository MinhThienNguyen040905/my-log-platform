package com.mylog.safety.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "safety_events")
class SafetyEvent {
    @Id UUID id;
    @Column(name = "user_id") UUID userId;
    @Column(name = "subject_ref", nullable = false) byte[] subjectRef;
    @Column(name = "journal_entry_id") UUID journalEntryId;
    @Column(name = "risk_level", nullable = false) String riskLevel;
    @Column(name = "decision", nullable = false) String decision;
    @Column(name = "rule_version") String ruleVersion;
    @Column(name = "classifier") String classifier;
    @Column(name = "classifier_version") String classifierVersion;
    @Column(name = "policy_version", nullable = false) String policyVersion;
    @Column(name = "confidence") BigDecimal confidence;
    @Column(name = "resource_set_version") String resourceSetVersion;
    @Column(name = "created_at", nullable = false) Instant createdAt;
}
