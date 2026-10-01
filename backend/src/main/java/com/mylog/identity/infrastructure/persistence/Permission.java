package com.mylog.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "permissions")
class Permission {
    @Id UUID id;
    @Column(name = "code", nullable = false) String code;
    @Column(name = "description") String description;
    @Column(name = "created_at", nullable = false) Instant createdAt;
}
