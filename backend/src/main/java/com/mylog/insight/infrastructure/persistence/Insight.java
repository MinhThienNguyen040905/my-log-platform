package com.mylog.insight.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "insights")
class Insight {
    @Id UUID id;
    @Column(name = "user_id", nullable = false) UUID userId;
    @Column(name = "insight_type", nullable = false) String insightType;
    @Column(name = "period_start", nullable = false) LocalDate periodStart;
    @Column(name = "period_end", nullable = false) LocalDate periodEnd;
    @Column(nullable = false) String timezone;
    @Column(nullable = false) String status;
    String direction;
    BigDecimal strength;
    @Column(name = "sample_size", nullable = false) int sampleSize;
    @Column(name = "algorithm_version", nullable = false) String algorithmVersion;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "metrics_snapshot", nullable = false, columnDefinition = "jsonb")
    Map<String, Object> metricsSnapshot;
    @Column(name = "encrypted_narrative") byte[] encryptedNarrative;
    @Column(name = "narrative_iv") byte[] narrativeIv;
    @Column(name = "narrative_wrapped_key") byte[] narrativeWrappedKey;
    @Column(name = "narrative_key_version") String narrativeKeyVersion;
    @Column(name = "generated_by", nullable = false) String generatedBy;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "superseded_at") Instant supersededAt;
}
