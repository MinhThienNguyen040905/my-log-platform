package com.mylog.platform.outbox;

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
@Table(name = "outbox_events")
class OutboxEvent {
    @Id UUID id;
    @Column(name = "aggregate_type", nullable = false) String aggregateType;
    @Column(name = "aggregate_id", nullable = false) UUID aggregateId;
    @Column(name = "event_type", nullable = false) String eventType;
    @Column(name = "event_version", nullable = false) short eventVersion;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    Map<String, Object> payload;
    @Column(name = "status", nullable = false) String status;
    @Column(name = "attempt", nullable = false) int attempt;
    @Column(name = "available_at", nullable = false) Instant availableAt;
    @Column(name = "locked_at") Instant lockedAt;
    @Column(name = "locked_by") String lockedBy;
    @Column(name = "lease_expires_at") Instant leaseExpiresAt;
    @Column(name = "last_error_code") String lastErrorCode;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "published_at") Instant publishedAt;
}
