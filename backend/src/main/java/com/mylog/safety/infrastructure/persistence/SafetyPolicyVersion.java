package com.mylog.safety.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "safety_policy_versions")
class SafetyPolicyVersion {
    @Id UUID id;
    @Column(name = "version", nullable = false) String version;
    @Column(name = "status", nullable = false) String status;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "config", nullable = false, columnDefinition = "jsonb")
    Map<String, Object> config;
    @Column(name = "approved_by") UUID approvedBy;
    @Column(name = "approved_at") Instant approvedAt;
    @Column(name = "effective_from") Instant effectiveFrom;
    @Column(name = "effective_to") Instant effectiveTo;
    @Column(name = "created_at", nullable = false) Instant createdAt;
}
