package com.mylog.safety.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "safety_events")
public class SafetyEvent {
    @Id public UUID id;
    @Column(name = "user_id") public UUID userId;
    @Column(name = "subject_ref", nullable = false) public byte[] subjectRef;
    @Column(name = "journal_entry_id") public UUID journalEntryId;
    @Column(name = "risk_level", nullable = false) public String riskLevel;
    @Column(name = "decision", nullable = false) public String decision;
    @Column(name = "rule_version") public String ruleVersion;
    @Column(name = "classifier") public String classifier;
    @Column(name = "classifier_version") public String classifierVersion;
    @Column(name = "policy_version", nullable = false) public String policyVersion;
    @Column(name = "confidence") public BigDecimal confidence;
    @Column(name = "resource_set_version") public String resourceSetVersion;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
}
