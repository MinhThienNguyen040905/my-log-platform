package com.mylog.identity.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "roles")
public class Role {
    @Id public UUID id;
    @Column(name = "code", nullable = false) public String code;
    @Column(name = "name", nullable = false) public String name;
    @Column(name = "description") public String description;
    @Column(name = "system_role", nullable = false) public boolean systemRole;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
}
