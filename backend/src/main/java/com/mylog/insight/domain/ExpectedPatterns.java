package com.mylog.insight.domain;

import java.util.Set;

/**
 * Associations almost everyone already knows about themselves (heavy work days feel worse, short nights
 * go with lower mood, weekends feel lighter). Shown as-is they read as mechanical, so the plain finding is
 * dropped; the same conditions still surface through {@link ContrastDetectors} when something about them
 * is unusual: days that went well anyway, or a change compared with the previous window.
 */
public final class ExpectedPatterns {
    /** Check-in activities whose days are commonly heavier. */
    public static final Set<String> HEAVY_ACTIVITIES = Set.of("meeting", "work-overtime");
    /** Analysis topics whose entries commonly carry difficult emotions. */
    public static final Set<String> HEAVY_TOPICS = Set.of("WORK");
    static final Set<String> DIFFICULT_EMOTIONS = Set.of("ANXIETY", "ANGER", "SADNESS", "FEAR");

    private ExpectedPatterns() {}

    public static boolean isExpected(PatternFinding finding) {
        var f = finding.facts();
        boolean up = "UP".equals(finding.direction());
        return switch (finding.type()) {
            case PatternDetectors.ACTIVITY_MOOD -> !up && HEAVY_ACTIVITIES.contains(String.valueOf(f.get("activity")));
            // More sleep with better mood; the reverse direction is unusual and kept.
            case PatternDetectors.SLEEP_MOOD -> up;
            case PatternDetectors.EMOTION_TOPIC -> HEAVY_TOPICS.contains(String.valueOf(f.get("topic")))
                    && DIFFICULT_EMOTIONS.contains(String.valueOf(f.get("emotion")));
            case PatternDetectors.WEEKDAY -> expectedWeekday(String.valueOf(f.get("weekday")),
                    "STRESS".equals(f.get("metric")), up);
            default -> false;
        };
    }

    /** Lighter weekends and heavier Mondays. */
    private static boolean expectedWeekday(String weekday, boolean stress, boolean up) {
        boolean lighter = stress ? !up : up;
        return switch (weekday) {
            case "SATURDAY", "SUNDAY" -> lighter;
            case "MONDAY" -> !lighter;
            default -> false;
        };
    }
}
