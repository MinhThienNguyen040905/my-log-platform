package com.mylog.export.api.response;
import com.mylog.export.application.query.ExportView;
import java.time.Instant;
import java.util.UUID;
public record ExportResponse(UUID id, String format, String status, Instant createdAt, Instant completedAt,
                             Instant expiresAt) {
    public static ExportResponse from(ExportView view) {
        return new ExportResponse(view.id(),view.format(),view.status(),view.createdAt(),view.completedAt(),view.expiresAt());
    }
}
