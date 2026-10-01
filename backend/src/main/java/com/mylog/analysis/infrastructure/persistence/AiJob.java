package com.mylog.analysis.infrastructure.persistence;

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
@Table(name = "ai_jobs")
public class AiJob {
    @Id public UUID id;
    @Column(name = "job_type", nullable = false) public String jobType;
    @Column(name = "aggregate_type", nullable = false) public String aggregateType;
    @Column(name = "aggregate_id", nullable = false) public UUID aggregateId;
    @Column(name = "user_id") public UUID userId;
    @Column(nullable = false) public String status;
    @Column(nullable = false) public short priority;
    @Column(nullable = false) public int attempt;
    @Column(name = "max_attempts", nullable = false) public int maxAttempts;
    @Column(name = "available_at", nullable = false) public Instant availableAt;
    @Column(name = "locked_at") public Instant lockedAt;
    @Column(name = "locked_by") public String lockedBy;
    @Column(name = "lease_expires_at") public Instant leaseExpiresAt;
    @Column(name = "idempotency_key", nullable = false) public String idempotencyKey;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") public Map<String, Object> payload;
    @Column(name = "payload_version", nullable = false) public short payloadVersion;
    @Column(name = "last_error_code") public String lastErrorCode;
    @Column(name = "last_error_summary") public String lastErrorSummary;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
    @Column(name = "started_at") public Instant startedAt;
    @Column(name = "finished_at") public Instant finishedAt;
}
