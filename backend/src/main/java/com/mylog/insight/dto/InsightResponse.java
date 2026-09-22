package com.mylog.insight.dto;

import com.mylog.insight.service.InsightView;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record InsightResponse(
        UUID id, String type, String title, String description, String confidence, String status,
        LocalDate periodStart, LocalDate periodEnd, Instant createdAt, Instant updatedAt,
        List<Evidence> evidence, List<Action> suggestedActions) {

    public static InsightResponse from(InsightView view) {
        return new InsightResponse(
                view.id(), view.type(), view.title(), view.description(), view.confidence(), view.status(),
                view.periodStart(), view.periodEnd(), view.createdAt(), view.updatedAt(),
                view.evidence().stream().map(item -> new Evidence(
                        item.id(), item.type(), item.sampleSize(), item.matchingCount(), item.metric(),
                        item.numericValue(), item.unit(), item.details(), item.calculationVersion())).toList(),
                view.suggestedActions().stream().map(item -> new Action(
                        item.id(), item.description(), item.status(), item.createdAt(), item.respondedAt())).toList());
    }

    public record Evidence(
            UUID id, String type, int sampleSize, Integer matchingCount, String metric,
            BigDecimal numericValue, String unit, Map<String, Object> details, String calculationVersion) {}

    public record Action(UUID id, String description, String status, Instant createdAt, Instant respondedAt) {}
}
