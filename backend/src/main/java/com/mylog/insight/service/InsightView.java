package com.mylog.insight.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record InsightView(
        UUID id, String type, String title, String description, String confidence, String status,
        LocalDate periodStart, LocalDate periodEnd, Instant createdAt, Instant updatedAt,
        List<Evidence> evidence, List<Action> suggestedActions) {

    public record Evidence(
            UUID id, String type, int sampleSize, Integer matchingCount, String metric,
            BigDecimal numericValue, String unit, Map<String, Object> details, String calculationVersion) {}

    public record Action(UUID id, String description, String status, Instant createdAt, Instant respondedAt) {}
}
