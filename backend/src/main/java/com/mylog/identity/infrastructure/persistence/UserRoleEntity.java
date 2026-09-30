package com.mylog.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_roles")
@IdClass(UserRoleId.class)
class UserRoleEntity {
    @Id @Column(name = "user_id") UUID userId;
    @Id @Column(name = "role_id") UUID roleId;
    @Column(name = "assigned_by") UUID assignedBy;
    @Column(name = "assigned_at", nullable = false) Instant assignedAt;
    @Column(name = "expires_at") Instant expiresAt;
}
