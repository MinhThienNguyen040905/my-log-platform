package com.mylog.identity.infrastructure.persistence.entity;

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
public class RolePermission {
    @Id @Column(name = "role_id") public UUID roleId;
    @Id @Column(name = "permission_id") public UUID permissionId;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
}
