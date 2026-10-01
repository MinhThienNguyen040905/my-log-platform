package com.mylog.user.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "deletion_requests")
public class DeletionRequest {
    @Id public UUID id;
    @Column(name = "user_id") public UUID userId;
    @Column(name = "subject_hash", nullable = false) public byte[] subjectHash;
    @Column(nullable = false) public String status;
    @Column(name = "requested_at", nullable = false) public Instant requestedAt;
    @Column(name = "scheduled_for", nullable = false) public Instant scheduledFor;
    @Column(name = "lease_expires_at") public Instant leaseExpiresAt;
    @Column(nullable = false) public int attempt;
    @Column(nullable = false) public String checkpoint;
    @Column(name = "last_error_code") public String lastErrorCode;
    @Column(name = "completed_at") public Instant completedAt;
    @Column(name = "cancelled_at") public Instant cancelledAt;
}
