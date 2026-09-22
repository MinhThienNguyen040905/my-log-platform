package com.mylog.analysis.port;

import java.math.BigDecimal;
import java.util.List;

public record AiAnalysisOutput(
        String schemaVersion,
        String sentiment,
        String riskLevel,
        List<Emotion> emotions,
        List<Topic> topics,
        String summary,
        String explanation,
        List<String> reflections,
        String provider,
        String model,
        String promptVersion,
        Integer inputTokens,
        Integer outputTokens,
        long latencyMs) {

    public record Emotion(String type, BigDecimal score) {}

    public record Topic(String name, BigDecimal confidence) {}
}
