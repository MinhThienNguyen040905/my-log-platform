package com.mylog.feedback.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "feedback")
public class Feedback {
    @Id public UUID id;
    @Column(name = "user_id", nullable = false) public UUID userId;
    @Column(nullable = false) public String category;
    @Column(nullable = false) public String status;
    @Column(name = "encrypted_message", nullable = false) public byte[] encryptedMessage;
    @Column(name = "message_iv", nullable = false) public byte[] messageIv;
    @Column(name = "message_wrapped_key", nullable = false) public byte[] messageWrappedKey;
    @Column(name = "message_key_version", nullable = false) public String messageKeyVersion;
    @Column(name = "assigned_to") public UUID assignedTo;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
    @Column(name = "updated_at", nullable = false) public Instant updatedAt;
    @Column(name = "resolved_at") public Instant resolvedAt;
    @Column(name = "expires_at", nullable = false) public Instant expiresAt;
    @Version @Column(name = "row_version", nullable = false) public long version;
}
