package com.mylog.reporting.infrastructure.persistence.entity;

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
@Table(name = "report_evidence")
public class ReportEvidence {
    @Id public UUID id;
    @Column(name = "report_id", nullable = false) public UUID reportId;
    @Column(name = "insight_id") public UUID insightId;
    @Column(name = "source_type", nullable = false) public String sourceType;
    @Column(name = "source_id") public UUID sourceId;
    @Column(name = "evidence_date", nullable = false) public LocalDate evidenceDate;
    @Column(name = "metric_name", nullable = false) public String metricName;
    @Column(name = "metric_value") public BigDecimal metricValue;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") public Map<String, Object> metadata;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
}
