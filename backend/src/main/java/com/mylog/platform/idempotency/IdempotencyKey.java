package com.mylog.platform.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys")
class IdempotencyKey {
    @Id UUID id;
    @Column(name = "user_id", nullable = false) UUID userId;
    @Column(name = "operation", nullable = false) String operation;
    @Column(name = "idempotency_key", nullable = false) String idempotencyKey;
    @Column(name = "request_hash", nullable = false) byte[] requestHash;
    @Column(name = "response_status") Integer responseStatus;
    @Column(name = "response_reference") UUID responseReference;
    @Column(name = "state", nullable = false) String state;
    @Column(name = "expires_at", nullable = false) Instant expiresAt;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "completed_at") Instant completedAt;
}
