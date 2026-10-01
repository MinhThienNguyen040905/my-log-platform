package com.mylog.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "role_permissions")
@IdClass(RolePermissionId.class)
class RolePermission {
    @Id @Column(name = "role_id") UUID roleId;
    @Id @Column(name = "permission_id") UUID permissionId;
    @Column(name = "created_at", nullable = false) Instant createdAt;
}
