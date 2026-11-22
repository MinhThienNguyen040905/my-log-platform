package com.mylog.insight.api.response;

import com.mylog.insight.application.query.ThinkingPatternView;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PatternResponse(UUID id, String code, String name, String narrative, String explanation,
                              List<String> steps, List<QuoteResponse> quotes, int entryCount, int weekCount,
                              LocalDate from, LocalDate to, String feedback, UUID planGoalId, Instant planStartedAt,
                              String generatedBy, UUID knowledgeVersionId, boolean contentReviewed,
                              Instant createdAt, Instant updatedAt) {
    public record QuoteResponse(String text, LocalDate date) {}

    public static PatternResponse from(ThinkingPatternView v) {
        return new PatternResponse(v.id(), v.code(), v.name(), v.narrative(), v.explanation(), v.steps(),
                v.quotes().stream().map(q -> new QuoteResponse(q.text(), q.date())).toList(), v.entryCount(),
                v.weekCount(), v.periodStart(), v.periodEnd(), v.feedback(), v.planGoalId(), v.planStartedAt(),
                v.generatedBy(), v.knowledgeVersionId(), v.contentReviewed(), v.createdAt(), v.updatedAt());
    }
}
