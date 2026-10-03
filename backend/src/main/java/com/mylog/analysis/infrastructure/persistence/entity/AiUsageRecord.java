package com.mylog.analysis.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_usage_records")
public class AiUsageRecord {
    @Id public UUID id;
    @Column(name = "job_id") public UUID jobId;
    @Column(name = "analysis_id") public UUID analysisId;
    @Column(nullable = false) public String provider;
    @Column(nullable = false) public String model;
    @Column(nullable = false) public String operation;
    @Column(name = "input_tokens", nullable = false) public int inputTokens;
    @Column(name = "output_tokens", nullable = false) public int outputTokens;
    @Column(name = "estimated_cost_usd", nullable = false) public BigDecimal estimatedCostUsd;
    @Column(name = "latency_ms", nullable = false) public int latencyMs;
    @Column(name = "request_status", nullable = false) public String requestStatus;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
}
