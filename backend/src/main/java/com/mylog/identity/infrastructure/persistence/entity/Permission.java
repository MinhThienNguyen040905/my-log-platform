package com.mylog.identity.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "permissions")
public class Permission {
    @Id public UUID id;
    @Column(name = "code", nullable = false) public String code;
    @Column(name = "description") public String description;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
}
