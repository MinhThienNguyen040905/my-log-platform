package com.mylog.export.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "export_requests")
public class ExportRequest {
    @Id public UUID id;
    @Column(name = "user_id", nullable = false) public UUID userId;
    @Column(nullable = false) public String format;
    @Column(nullable = false) public String status;
    @Column(name = "encrypted_file") public byte[] encryptedFile;
    @Column(name = "file_iv") public byte[] fileIv;
    @Column(name = "file_wrapped_key") public byte[] fileWrappedKey;
    @Column(name = "file_key_version") public String fileKeyVersion;
    @Column(name = "file_sha256") public byte[] fileSha256;
    @Column(name = "file_size_bytes") public Long fileSizeBytes;
    @Column(nullable = false) public int attempt;
    @Column(name = "available_at", nullable = false) public Instant availableAt;
    @Column(name = "lease_expires_at") public Instant leaseExpiresAt;
    @Column(name = "last_error_code") public String lastErrorCode;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
    @Column(name = "completed_at") public Instant completedAt;
    @Column(name = "expires_at") public Instant expiresAt;
}
