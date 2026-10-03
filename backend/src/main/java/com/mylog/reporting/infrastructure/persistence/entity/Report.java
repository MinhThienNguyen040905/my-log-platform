package com.mylog.reporting.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "reports")
public class Report {
    @Id public UUID id;
    @Column(name = "user_id", nullable = false) public UUID userId;
    @Column(name = "report_type", nullable = false) public String reportType;
    @Column(name = "period_start", nullable = false) public LocalDate periodStart;
    @Column(name = "period_end", nullable = false) public LocalDate periodEnd;
    @Column(nullable = false) public String timezone;
    @Column(nullable = false) public int version;
    @Column(nullable = false) public String status;
    @Column(name = "sample_size", nullable = false) public int sampleSize;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "metrics_snapshot", nullable = false, columnDefinition = "jsonb")
    public Map<String, Object> metricsSnapshot;
    @Column(name = "encrypted_narrative") public byte[] encryptedNarrative;
    @Column(name = "narrative_iv") public byte[] narrativeIv;
    @Column(name = "narrative_wrapped_key") public byte[] narrativeWrappedKey;
    @Column(name = "narrative_key_version") public String narrativeKeyVersion;
    @Column(name = "generated_by", nullable = false) public String generatedBy;
    public String model;
    @Column(name = "prompt_template_version") public String promptTemplateVersion;
    @Column(name = "safety_policy_version") public String safetyPolicyVersion;
    @Column(nullable = false) public int attempt;
    @Column(name = "available_at", nullable = false) public Instant availableAt;
    @Column(name = "locked_at") public Instant lockedAt;
    @Column(name = "locked_by") public String lockedBy;
    @Column(name = "lease_expires_at") public Instant leaseExpiresAt;
    @Column(name = "last_error_code") public String lastErrorCode;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
    @Column(name = "completed_at") public Instant completedAt;
}
