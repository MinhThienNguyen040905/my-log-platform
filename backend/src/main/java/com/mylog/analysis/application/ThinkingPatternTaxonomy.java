package com.mylog.analysis.application;

import java.util.List;
import java.util.Set;

/**
 * Versioned list of thinking-pattern codes (groups A and B of docs/THINKING_PATTERN_RECOGNITION_PLAN.md).
 * The database only checks the code shape; this class decides which codes a taxonomy version accepts.
 */
public final class ThinkingPatternTaxonomy {
    public static final String VERSION = "tp-v1";

    /** Thinking traps (group A) followed by loops and habits (group B). */
    public static final List<String> CODES = List.of(
            "CATASTROPHIZING", "ALL_OR_NOTHING", "OVERGENERALIZATION", "MIND_READING", "SHOULD_STATEMENTS",
            "SELF_LABELING", "PERSONALIZATION", "DISCOUNTING_POSITIVE", "EMOTIONAL_REASONING",
            "RUMINATION", "WORRY_LOOP", "AVOIDANCE", "PROCRASTINATION_GUILT", "PERFECTIONISM",
            "PEOPLE_PLEASING", "HARSH_SELF_CRITICISM", "SOCIAL_WITHDRAWAL");

    private static final Set<String> KNOWN = Set.copyOf(CODES);

    private ThinkingPatternTaxonomy() {}

    public static boolean isKnown(String code) {
        return code != null && KNOWN.contains(code);
    }
}
