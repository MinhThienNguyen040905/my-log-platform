package com.mylog.analysis.application;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

public final class AnalysisOutputValidator {
    private static final Set<String> SENTIMENT = Set.of("POSITIVE", "NEUTRAL", "NEGATIVE", "MIXED");
    private static final Set<String> EMOTIONS = Set.of("JOY", "SADNESS", "ANGER", "FEAR", "CALM", "HOPE", "ANXIETY");
    private static final Set<String> TOPICS = Set.of("WORK", "RELATIONSHIPS", "HEALTH", "SLEEP", "SELF_CARE", "OTHER");

    private AnalysisOutputValidator() {}

    public static void validate(JournalAnalyzer.Result output) {
        if (output == null || !SENTIMENT.contains(output.sentiment()) || !score(output.sentimentScore())
                || output.emotions() == null || output.emotions().size() > 5
                || output.topics() == null || output.topics().size() > 5
                || output.reflection() == null || output.reflection().isBlank() || output.reflection().length() > 2000
                || output.inputTokens() < 0 || output.outputTokens() < 0
                || output.estimatedCostUsd() == null || output.estimatedCostUsd().signum() < 0
                || output.provider() == null || !output.provider().matches("[A-Za-z0-9._:-]{1,40}")
                || output.model() == null || !output.model().matches("[A-Za-z0-9._:-]{1,120}")
                || output.promptVersion() == null || !output.promptVersion().matches("[A-Za-z0-9._:-]{1,40}"))
            throw new InvalidAnalysisOutputException();
        checkCodes(output.emotions(), EMOTIONS);
        checkCodes(output.topics(), TOPICS);
        String lower = output.reflection().toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("hotline") || lower.contains("đường dây nóng")
                || lower.contains("chẩn đoán") || lower.contains("diagnos")
                || lower.contains("tự tử") || lower.contains("suicide"))
            throw new InvalidAnalysisOutputException();
    }

    private static void checkCodes(java.util.List<JournalAnalyzer.ScoredCode> values, Set<String> allowed) {
        Set<String> seen = new HashSet<>();
        for (var value : values) {
            if (value == null || !allowed.contains(value.code()) || !score(value.score()) || !seen.add(value.code()))
                throw new InvalidAnalysisOutputException();
        }
    }

    private static boolean score(BigDecimal score) {
        return score != null && score.compareTo(BigDecimal.ZERO) >= 0
                && score.compareTo(BigDecimal.ONE) <= 0 && score.scale() <= 5;
    }

    public static final class InvalidAnalysisOutputException extends RuntimeException {}
}
