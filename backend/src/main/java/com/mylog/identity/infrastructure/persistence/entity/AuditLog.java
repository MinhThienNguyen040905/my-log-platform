package com.mylog.identity.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id public UUID id;
    @Column(name = "actor_user_id") public UUID actorUserId;
    @Column(name = "actor_type", nullable = false) public String actorType;
    @Column(name = "action", nullable = false) public String action;
    @Column(name = "target_type", nullable = false) public String targetType;
    @Column(name = "target_id") public UUID targetId;
    @Column(name = "reason_code") public String reasonCode;
    @Column(name = "trace_id") public String traceId;
    @Column(name = "occurred_at", nullable = false) public Instant occurredAt;
}
