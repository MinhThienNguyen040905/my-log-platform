package com.mylog.user.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_consents")
public class UserConsent {
    @Id public UUID id;
    @Column(name = "user_id", nullable = false) public UUID userId;
    @Column(name = "consent_type", nullable = false) public String consentType;
    @Column(name = "document_version", nullable = false) public String documentVersion;
    @Column(name = "granted", nullable = false) public boolean granted;
    @Column(name = "decided_at", nullable = false) public Instant decidedAt;
    @Column(name = "source", nullable = false) public String source;
}
