package com.mylog.analysis.api.response;

import com.mylog.analysis.application.query.AnalysisView;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AnalysisResponse(UUID entryId, int contentVersion, String status, UUID analysisId,
                               String sentiment, BigDecimal sentimentScore, List<String> emotions,
                               List<String> topics, String reflection) {
    public static AnalysisResponse from(AnalysisView view) {
        return new AnalysisResponse(view.entryId(), view.contentVersion(), view.status(), view.analysisId(),
                view.sentiment(), view.sentimentScore(), view.emotions(), view.topics(), view.reflection());
    }
}
