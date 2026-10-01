package com.mylog.insight.api.response;

import com.mylog.insight.application.query.InsightEvidenceView;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InsightEvidenceResponse(String source, LocalDate date, String metric, BigDecimal value) {
    public static InsightEvidenceResponse from(InsightEvidenceView view) {
        return new InsightEvidenceResponse(view.source(), view.date(), view.metric(), view.value());
    }
}
