package com.mylog.reporting.api.response;

import com.mylog.reporting.application.query.ReportView;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ReportResponse(UUID id, String type, LocalDate from, LocalDate to, String timezone,
                             int version, String status, int sampleSize, Map<String, Object> metrics,
                             String narrative, String generatedBy, List<ReportEvidenceResponse> evidence,
                             Map<String, String> entryTitles, Instant createdAt, Instant completedAt) {
    public static ReportResponse from(ReportView view) {
        return new ReportResponse(view.id(), view.type(), view.from(), view.to(), view.timezone(),
                view.version(), view.status(), view.sampleSize(), view.metrics(), view.narrative(),
                view.generatedBy(), view.evidence().stream().map(ReportEvidenceResponse::from).toList(),
                view.entryTitles(), view.createdAt(), view.completedAt());
    }
}
