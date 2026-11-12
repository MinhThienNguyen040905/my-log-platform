package com.mylog.insight.application;

import com.mylog.insight.application.query.InsightPage;
import com.mylog.insight.domain.MoodSleepAssociation;
import com.mylog.insight.domain.PatternFinding;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InsightStore {
    /** A finding plus the narrative chosen for it; {@code generatedBy} is STATISTICAL or AI_ASSISTED. */
    record PatternInsight(PatternFinding finding, String narrative, String generatedBy, String narrativeModel) {}

    record NarrationUsage(String provider, String model, int inputTokens, int outputTokens,
                          BigDecimal estimatedCostUsd, long latencyMs, boolean succeeded) {}

    void saveAssociation(UUID userId, LocalDate from, LocalDate to, String timezone,
                         MoodSleepAssociation.Result result, Instant now);

    /**
     * Replaces the user's pattern insights of {@code algorithmVersion}: earlier ACTIVE ones become SUPERSEDED,
     * a previous run for the same window is removed, and {@code insights} are stored with their evidence.
     */
    void replacePatterns(UUID userId, LocalDate from, LocalDate to, String timezone, String algorithmVersion,
                         List<PatternInsight> insights, Instant now);

    /** Owner-scoped; false when the insight does not exist for this user. */
    boolean hide(UUID userId, UUID insightId);

    void recordNarrationUsage(NarrationUsage usage, Instant now);

    InsightPage list(UUID userId, LocalDate from, LocalDate to, String cursor, int limit);

    record UserZone(UUID userId, String timezone) {}

    /** Active users with a profile, ordered by ID, for the scheduled refresh. */
    List<UserZone> activeUsersAfter(UUID cursor, int limit);

    /**
     * True when the user has no pattern insights of {@code algorithmVersion} created since {@code since}
     * and has journal/check-in changes after the last such refresh (or never had one).
     */
    boolean needsPatternRefresh(UUID userId, String algorithmVersion, Instant since);
}
