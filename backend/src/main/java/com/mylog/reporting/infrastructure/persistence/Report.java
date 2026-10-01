package com.mylog.reporting.infrastructure.persistence;

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
class Report {
    @Id UUID id;
    @Column(name = "user_id", nullable = false) UUID userId;
    @Column(name = "report_type", nullable = false) String reportType;
    @Column(name = "period_start", nullable = false) LocalDate periodStart;
    @Column(name = "period_end", nullable = false) LocalDate periodEnd;
    @Column(nullable = false) String timezone;
    @Column(nullable = false) int version;
    @Column(nullable = false) String status;
    @Column(name = "sample_size", nullable = false) int sampleSize;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "metrics_snapshot", nullable = false, columnDefinition = "jsonb")
    Map<String, Object> metricsSnapshot;
    @Column(name = "encrypted_narrative") byte[] encryptedNarrative;
    @Column(name = "narrative_iv") byte[] narrativeIv;
    @Column(name = "narrative_wrapped_key") byte[] narrativeWrappedKey;
    @Column(name = "narrative_key_version") String narrativeKeyVersion;
    @Column(name = "generated_by", nullable = false) String generatedBy;
    String model;
    @Column(name = "prompt_template_version") String promptTemplateVersion;
    @Column(name = "safety_policy_version") String safetyPolicyVersion;
    @Column(nullable = false) int attempt;
    @Column(name = "available_at", nullable = false) Instant availableAt;
    @Column(name = "locked_at") Instant lockedAt;
    @Column(name = "locked_by") String lockedBy;
    @Column(name = "lease_expires_at") Instant leaseExpiresAt;
    @Column(name = "last_error_code") String lastErrorCode;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "completed_at") Instant completedAt;
}
