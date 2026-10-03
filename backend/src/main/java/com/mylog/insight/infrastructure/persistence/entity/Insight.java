package com.mylog.insight.infrastructure.persistence.entity;

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
public class Insight {
    @Id public UUID id;
    @Column(name = "user_id", nullable = false) public UUID userId;
    @Column(name = "insight_type", nullable = false) public String insightType;
    @Column(name = "period_start", nullable = false) public LocalDate periodStart;
    @Column(name = "period_end", nullable = false) public LocalDate periodEnd;
    @Column(nullable = false) public String timezone;
    @Column(nullable = false) public String status;
    public String direction;
    public BigDecimal strength;
    @Column(name = "sample_size", nullable = false) public int sampleSize;
    @Column(name = "algorithm_version", nullable = false) public String algorithmVersion;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "metrics_snapshot", nullable = false, columnDefinition = "jsonb")
    public Map<String, Object> metricsSnapshot;
    @Column(name = "encrypted_narrative") public byte[] encryptedNarrative;
    @Column(name = "narrative_iv") public byte[] narrativeIv;
    @Column(name = "narrative_wrapped_key") public byte[] narrativeWrappedKey;
    @Column(name = "narrative_key_version") public String narrativeKeyVersion;
    @Column(name = "generated_by", nullable = false) public String generatedBy;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
    @Column(name = "superseded_at") public Instant supersededAt;
}
