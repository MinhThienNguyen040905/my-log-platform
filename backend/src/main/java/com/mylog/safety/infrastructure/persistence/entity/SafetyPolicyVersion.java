package com.mylog.safety.infrastructure.persistence.entity;

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
public class SafetyPolicyVersion {
    @Id public UUID id;
    @Column(name = "version", nullable = false) public String version;
    @Column(name = "status", nullable = false) public String status;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "config", nullable = false, columnDefinition = "jsonb")
    public Map<String, Object> config;
    @Column(name = "approved_by") public UUID approvedBy;
    @Column(name = "approved_at") public Instant approvedAt;
    @Column(name = "effective_from") public Instant effectiveFrom;
    @Column(name = "effective_to") public Instant effectiveTo;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
}
