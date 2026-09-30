package com.mylog.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
class AuditLogEntity {
    @Id UUID id;
    @Column(name = "actor_user_id") UUID actorUserId;
    @Column(name = "actor_type", nullable = false) String actorType;
    @Column(name = "action", nullable = false) String action;
    @Column(name = "target_type", nullable = false) String targetType;
    @Column(name = "target_id") UUID targetId;
    @Column(name = "reason_code") String reasonCode;
    @Column(name = "trace_id") String traceId;
    @Column(name = "occurred_at", nullable = false) Instant occurredAt;
}
