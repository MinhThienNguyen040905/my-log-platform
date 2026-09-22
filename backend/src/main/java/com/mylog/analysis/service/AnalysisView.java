package com.mylog.analysis.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnalysisView(
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
}
