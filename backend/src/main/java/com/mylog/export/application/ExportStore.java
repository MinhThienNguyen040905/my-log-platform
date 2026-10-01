package com.mylog.export.application;

import com.mylog.export.application.query.ExportView;
import com.mylog.platform.crypto.SensitiveDataCipher;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ExportStore {
    ExportView create(UUID id, UUID userId, String format, Instant now);
    Optional<ExportView> find(UUID userId, UUID id);
    Optional<Work> claim(Instant now);
    void ready(UUID id, UUID userId, SensitiveDataCipher.Encrypted encrypted, byte[] sha256,
               long size, Instant now, Instant expiresAt);
    void fail(UUID id, Instant retryAt, boolean terminal);
    Optional<Artifact> artifact(UUID userId, UUID id, Instant now);
    void expire(Instant now);
    record Work(UUID id, UUID userId, String format, int attempt) {}
    record Artifact(String format, SensitiveDataCipher.Encrypted encrypted, byte[] sha256, Instant expiresAt) {}
}
