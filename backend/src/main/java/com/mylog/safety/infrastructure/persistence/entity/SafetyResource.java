package com.mylog.safety.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "safety_resources")
public class SafetyResource {
    @Id public UUID id;
    @Column(name = "locale", nullable = false) public String locale;
    @Column(name = "country_code", nullable = false, length = 2) public String countryCode;
    @Column(name = "resource_type", nullable = false) public String resourceType;
    @Column(name = "name", nullable = false) public String name;
    @Column(name = "contact_value") public String contactValue;
    @Column(name = "description") public String description;
    @Column(name = "source_url") public String sourceUrl;
    @Column(name = "verified_at") public Instant verifiedAt;
    @Column(name = "status", nullable = false) public String status;
    @Column(name = "version", nullable = false) public int version;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
    @Column(name = "updated_at", nullable = false) public Instant updatedAt;
}
