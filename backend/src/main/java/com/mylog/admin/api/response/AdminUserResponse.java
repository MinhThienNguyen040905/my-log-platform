package com.mylog.admin.api.response;

import com.mylog.identity.application.AdminAccountStore;
import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(UUID id, String status, Instant createdAt, Instant lastLoginAt, long version) {
    public static AdminUserResponse from(AdminAccountStore.Metadata m) {
        return new AdminUserResponse(m.id(),m.status(),m.createdAt(),m.lastLoginAt(),m.version());
    }
}
