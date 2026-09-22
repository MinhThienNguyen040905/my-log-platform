package com.mylog.analysis.dto;

import com.mylog.analysis.service.AnalysisView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnalysisResponse(
        UUID journalId,
        long journalVersion,
        String status,
        Result result,
        Safety safety) {

    public record Result(
            UUID analysisId,
            String sentiment,
            String riskLevel,
            String summary,
            String explanation,
            List<Emotion> emotions,
            List<Topic> topics,
            String provider,
            String model,
            String promptVersion,
            Instant analyzedAt) {}

    public record Emotion(String type, BigDecimal originalScore, BigDecimal effectiveScore, boolean corrected) {}
    public record Topic(String name, BigDecimal confidence, String source) {}
    public record Safety(String riskLevel, boolean blocksNormalResponse, String actionTaken) {}

    public static AnalysisResponse from(AnalysisView view) {
        Result result = view.result() == null ? null : new Result(
                view.result().analysisId(), view.result().sentiment(), view.result().riskLevel(),
                view.result().summary(), view.result().explanation(),
                view.result().emotions().stream().map(item -> new Emotion(
                        item.type(), item.originalScore(), item.effectiveScore(), item.corrected())).toList(),
                view.result().topics().stream().map(item -> new Topic(
                        item.name(), item.confidence(), item.source())).toList(),
                view.result().provider(), view.result().model(), view.result().promptVersion(),
                view.result().analyzedAt());
        Safety safety = new Safety(
                view.safety().riskLevel(), view.safety().blocksNormalResponse(), view.safety().actionTaken());
        return new AnalysisResponse(view.journalId(), view.journalVersion(), view.status(), result, safety);
    }
}
