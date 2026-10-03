package com.mylog.selfcare.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "habits")
public class Habit {
    @Id public UUID id;
    @Column(name = "goal_id", nullable = false) public UUID goalId;
    @Column(name = "user_id", nullable = false) public UUID userId;
    @Column(name = "encrypted_title", nullable = false) public byte[] encryptedTitle;
    @Column(name = "title_iv", nullable = false) public byte[] titleIv;
    @Column(name = "title_wrapped_key", nullable = false) public byte[] titleWrappedKey;
    @Column(name = "title_key_version", nullable = false) public String titleKeyVersion;
    @Column(name = "target_value", nullable = false) public BigDecimal targetValue;
    @Column(nullable = false) public String unit;
    @Column(name = "frequency_type", nullable = false) public String frequencyType;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "frequency_config", nullable = false, columnDefinition = "jsonb") public Map<String, Object> frequencyConfig;
    @Column(nullable = false) public String timezone;
    @Column(nullable = false) public String status;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
    @Column(name = "updated_at", nullable = false) public Instant updatedAt;
    @Column(name = "row_version", nullable = false) public long rowVersion;
}
