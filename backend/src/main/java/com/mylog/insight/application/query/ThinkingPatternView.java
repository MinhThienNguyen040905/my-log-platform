package com.mylog.insight.application.query;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A live thinking-pattern card. {@code contentReviewed} is false while the explanation and steps come from the
 * draft texts rather than an approved knowledge article.
 */
public record ThinkingPatternView(UUID id, String code, String name, String narrative, String explanation,
                                  List<String> steps, List<Quote> quotes, int entryCount, int weekCount,
                                  LocalDate periodStart, LocalDate periodEnd, String feedback, UUID planGoalId,
                                  Instant planStartedAt, String generatedBy, UUID knowledgeVersionId,
                                  boolean contentReviewed, Instant createdAt, Instant updatedAt) {
    public record Quote(String text, LocalDate date) {}
}
