package com.mylog.safety.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "safety_resources")
class SafetyResource {
    @Id UUID id;
    @Column(name = "locale", nullable = false) String locale;
    @Column(name = "country_code", nullable = false, length = 2) String countryCode;
    @Column(name = "resource_type", nullable = false) String resourceType;
    @Column(name = "name", nullable = false) String name;
    @Column(name = "contact_value") String contactValue;
    @Column(name = "description") String description;
    @Column(name = "source_url") String sourceUrl;
    @Column(name = "verified_at") Instant verifiedAt;
    @Column(name = "status", nullable = false) String status;
    @Column(name = "version", nullable = false) int version;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;
}
