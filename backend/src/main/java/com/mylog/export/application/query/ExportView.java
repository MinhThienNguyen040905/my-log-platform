package com.mylog.export.application.query;

import java.time.Instant;
import java.util.UUID;

public record ExportView(UUID id, String format, String status, Instant createdAt, Instant completedAt,
                         Instant expiresAt) {}
