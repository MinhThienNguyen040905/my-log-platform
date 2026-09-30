package com.mylog.user.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_consents")
class UserConsentEntity {
    @Id UUID id;
    @Column(name = "user_id", nullable = false) UUID userId;
    @Column(name = "consent_type", nullable = false) String consentType;
    @Column(name = "document_version", nullable = false) String documentVersion;
    @Column(name = "granted", nullable = false) boolean granted;
    @Column(name = "decided_at", nullable = false) Instant decidedAt;
    @Column(name = "source", nullable = false) String source;
}
