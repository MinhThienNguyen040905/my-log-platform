package com.mylog.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_consents")
public class UserConsent {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "consent_type", nullable = false, length = 50)
    private String consentType;

    @Column(name = "policy_version", nullable = false, length = 30)
    private String policyVersion;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(nullable = false, length = 30)
    private String source;

    protected UserConsent() {}

    public UserConsent(
            UUID id,
            UUID userId,
            String consentType,
            String policyVersion,
            Instant grantedAt) {
        this.id = id;
        this.userId = userId;
        this.consentType = consentType;
        this.policyVersion = policyVersion;
        this.grantedAt = grantedAt;
        this.source = "WEB";
    }
}
