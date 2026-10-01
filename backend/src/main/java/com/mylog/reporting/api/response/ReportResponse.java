package com.mylog.reporting.api.response;

import com.mylog.reporting.application.query.ReportView;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ReportResponse(UUID id, String type, LocalDate from, LocalDate to, String timezone,
                             int version, String status, int sampleSize, Map<String, Object> metrics,
                             String narrative, List<ReportEvidenceResponse> evidence,
                             Instant createdAt, Instant completedAt) {
    public static ReportResponse from(ReportView view) {
        return new ReportResponse(view.id(), view.type(), view.from(), view.to(), view.timezone(),
                view.version(), view.status(), view.sampleSize(), view.metrics(), view.narrative(),
                view.evidence().stream().map(ReportEvidenceResponse::from).toList(),
                view.createdAt(), view.completedAt());
    }
}
