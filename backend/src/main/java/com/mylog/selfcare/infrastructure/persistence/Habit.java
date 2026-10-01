package com.mylog.selfcare.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "habits")
class Habit {
    @Id UUID id;
    @Column(name = "goal_id", nullable = false) UUID goalId;
    @Column(name = "user_id", nullable = false) UUID userId;
    @Column(name = "encrypted_title", nullable = false) byte[] encryptedTitle;
    @Column(name = "title_iv", nullable = false) byte[] titleIv;
    @Column(name = "title_wrapped_key", nullable = false) byte[] titleWrappedKey;
    @Column(name = "title_key_version", nullable = false) String titleKeyVersion;
    @Column(name = "target_value", nullable = false) BigDecimal targetValue;
    @Column(nullable = false) String unit;
    @Column(name = "frequency_type", nullable = false) String frequencyType;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "frequency_config", nullable = false, columnDefinition = "jsonb") Map<String, Object> frequencyConfig;
    @Column(nullable = false) String timezone;
    @Column(nullable = false) String status;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;
    @Column(name = "row_version", nullable = false) long rowVersion;
}
