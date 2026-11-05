package com.mylog.identity.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_oauth_identities")
public class UserOAuthIdentity {
    public UserOAuthIdentity() {}
    @Id
    public UUID id;

    @Column(name = "user_id", nullable = false)
    public UUID userId;

    @Column(name = "provider", nullable = false)
    public String provider;

    @Column(name = "provider_user_id", nullable = false)
    public String providerUserId;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}

