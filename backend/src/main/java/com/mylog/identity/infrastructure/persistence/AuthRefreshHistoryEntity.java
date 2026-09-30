package com.mylog.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_refresh_history")
class AuthRefreshHistoryEntity {
    @Id @Column(name = "token_hash") byte[] tokenHash;
    @Column(name = "session_id", nullable = false) UUID sessionId;
    @Column(name = "used_at", nullable = false) Instant usedAt;
}
