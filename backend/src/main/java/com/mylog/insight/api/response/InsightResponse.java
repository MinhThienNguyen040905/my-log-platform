package com.mylog.insight.api.response;

import com.mylog.insight.application.query.InsightView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InsightResponse(UUID id, String type, LocalDate from, LocalDate to, String timezone,
                              String direction, BigDecimal strength, int sampleSize,
                              String algorithmVersion, String narrative,
                              List<InsightEvidenceResponse> evidence) {
    public static InsightResponse from(InsightView view) {
        return new InsightResponse(view.id(), view.type(), view.from(), view.to(), view.timezone(),
                view.direction(), view.strength(), view.sampleSize(), view.algorithmVersion(),
                view.narrative(), view.evidence().stream().map(InsightEvidenceResponse::from).toList());
    }
}
