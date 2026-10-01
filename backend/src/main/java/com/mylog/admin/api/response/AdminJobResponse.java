package com.mylog.admin.api.response;

import com.mylog.analysis.application.AdminJobStore;
import java.time.Instant;
import java.util.UUID;

public record AdminJobResponse(UUID id, UUID userId, String type, String status, int attempt,
                               String errorCode, Instant createdAt, Instant availableAt) {
    public static AdminJobResponse from(AdminJobStore.Metadata m) {
        return new AdminJobResponse(m.id(),m.userId(),m.type(),m.status(),m.attempt(),
                m.errorCode(),m.createdAt(),m.availableAt());
    }
}
