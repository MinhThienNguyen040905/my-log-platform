package com.mylog.identity.infrastructure.persistence.entity;

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
public class UserRole {
    @Id @Column(name = "user_id") public UUID userId;
    @Id @Column(name = "role_id") public UUID roleId;
    @Column(name = "assigned_by") public UUID assignedBy;
    @Column(name = "assigned_at", nullable = false) public Instant assignedAt;
    @Column(name = "expires_at") public Instant expiresAt;
}
