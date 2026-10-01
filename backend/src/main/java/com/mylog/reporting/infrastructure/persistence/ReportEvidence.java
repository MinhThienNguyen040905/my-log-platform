package com.mylog.reporting.infrastructure.persistence;

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
class ReportEvidence {
    @Id UUID id;
    @Column(name = "report_id", nullable = false) UUID reportId;
    @Column(name = "insight_id") UUID insightId;
    @Column(name = "source_type", nullable = false) String sourceType;
    @Column(name = "source_id") UUID sourceId;
    @Column(name = "evidence_date", nullable = false) LocalDate evidenceDate;
    @Column(name = "metric_name", nullable = false) String metricName;
    @Column(name = "metric_value") BigDecimal metricValue;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") Map<String, Object> metadata;
    @Column(name = "created_at", nullable = false) Instant createdAt;
}
