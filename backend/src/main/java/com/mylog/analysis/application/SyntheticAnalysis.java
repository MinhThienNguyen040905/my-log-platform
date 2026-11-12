package com.mylog.analysis.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Local-development stand-in for a journal analyzer (option 1a). Derives emotions/topics from the
 * structured fields the user already entered (mood icon, scores, sleep, tags) — never from journal text —
 * so dashboards and pattern insights can be built before an approved analyzer exists.
 * Deterministic per entry so re-running a seed gives the same result.
 */
public final class SyntheticAnalysis {
    public static final String PROVIDER = "LOCAL_FAKE";
    public static final String MODEL = "synthetic-from-scores";
    public static final String PROMPT_VERSION = "dev-seed-v1";
    private static final String REFLECTION =
            "Đây là kết quả phân tích mẫu trong môi trường phát triển, được suy ra từ điểm số và thẻ bạn đã chọn.";

    private SyntheticAnalysis() {}

    public record Input(UUID entryId, String moodCode, BigDecimal moodScore, BigDecimal stressScore,
                        Integer sleepMinutes, Set<String> topicHints) {}

    public static JournalAnalyzer.Result generate(Input input) {
        Random random = new Random(input.entryId().getMostSignificantBits() ^ input.entryId().getLeastSignificantBits());
        double mood = input.moodScore() == null ? 6 : input.moodScore().doubleValue();
        double stress = input.stressScore() == null ? 5 : input.stressScore().doubleValue();

        // The user's own mood icon decides the primary emotion so analysis rarely contradicts it.
        String primary = switch (input.moodCode() == null ? "" : input.moodCode()) {
            case "happy" -> mood >= 8 || random.nextBoolean() ? "JOY" : "HOPE";
            case "neutral" -> stress >= 6.5 ? "ANXIETY" : "CALM";
            case "sad", "very-bad" -> "SADNESS";
            case "angry" -> stress >= 7 && random.nextBoolean() ? "ANXIETY" : "ANGER";
            default -> mood >= 7 ? "JOY" : mood <= 4 ? "SADNESS" : "CALM";
        };
        // Secondary tones must stay compatible with the icon: no hope on a sad day, no fear on a happy one.
        String icon = input.moodCode() == null ? "" : input.moodCode();
        boolean positiveIcon = icon.equals("happy");
        boolean negativeIcon = icon.equals("sad") || icon.equals("very-bad") || icon.equals("angry");
        Set<String> emotions = new LinkedHashSet<>();
        emotions.add(primary);
        if (stress >= 6.5) emotions.add("ANXIETY");
        if (positiveIcon && stress < 5) emotions.add(primary.equals("JOY") ? "CALM" : "JOY");
        if (icon.equals("neutral") && stress < 6.5 && random.nextInt(6) == 0) emotions.add("HOPE");
        if (negativeIcon && mood <= 4 && stress >= 7 && random.nextInt(3) == 0) emotions.add("FEAR");

        Set<String> topics = new LinkedHashSet<>(input.topicHints());
        if (input.sleepMinutes() != null && input.sleepMinutes() < 360) topics.add("SLEEP");
        if (topics.isEmpty()) topics.add("OTHER");

        boolean mixed = emotions.contains("ANXIETY") && (emotions.contains("JOY") || emotions.contains("HOPE"));
        String sentiment = mixed ? "MIXED" : mood >= 7 ? "POSITIVE" : mood <= 4.5 ? "NEGATIVE" : "NEUTRAL";

        return new JournalAnalyzer.Result(sentiment, score(Math.max(0, Math.min(1, mood / 10))),
                scored(emotions, random, 3), scored(topics, random, 3), REFLECTION,
                PROVIDER, MODEL, null, PROMPT_VERSION, 0, 0, BigDecimal.ZERO);
    }

    private static List<JournalAnalyzer.ScoredCode> scored(Set<String> codes, Random random, int limit) {
        List<JournalAnalyzer.ScoredCode> out = new ArrayList<>();
        double top = 0.65 + random.nextDouble() * 0.3;
        for (String code : codes) {
            if (out.size() == limit) break;
            out.add(new JournalAnalyzer.ScoredCode(code, score(top)));
            top = Math.max(0.2, top - 0.15 - random.nextDouble() * 0.15);
        }
        return out;
    }

    private static BigDecimal score(double value) {
        return BigDecimal.valueOf(value).setScale(5, RoundingMode.HALF_UP);
    }
}
