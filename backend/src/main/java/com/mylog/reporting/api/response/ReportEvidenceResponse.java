package com.mylog.reporting.api.response;

import com.mylog.reporting.application.query.ReportEvidenceView;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReportEvidenceResponse(String source, LocalDate date, String metric, BigDecimal value) {
    public static ReportEvidenceResponse from(ReportEvidenceView view) {
        return new ReportEvidenceResponse(view.source(), view.date(), view.metric(), view.value());
    }
}
