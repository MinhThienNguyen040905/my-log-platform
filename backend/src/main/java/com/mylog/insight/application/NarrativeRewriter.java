package com.mylog.insight.application;

import com.mylog.safety.application.SafetyScreeningUseCase;
import com.mylog.user.application.UserProfileUseCase;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Optional AI rewording of template sentences, shared by pattern insights and period reports. Statistics
 * and templates stay the source of truth: a rewrite is used only when a narrator is configured, the user
 * has AI_PROCESSING consent at this moment, no entry in the window was screened HIGH/CRITICAL, and the
 * text passes {@link InsightNarrativeValidator} and outbound safety screening. Must not run inside a
 * database transaction (it may call a provider).
 */
@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class NarrativeRewriter {
    /**
     * {@code text} may contain {@link InsightTemplates#TAG_PLACEHOLDER} standing in for user-written text
     * ({@code placeholderValue}); only the placeholder is ever sent.
     */
    public record Draft(String key, String type, String direction, Map<String, Object> facts,
                        String text, String placeholderValue) {}

    public record Rewrite(String text, String model) {}

    private final ObjectProvider<InsightNarrator> narrator;
    private final UserProfileUseCase users;
    private final DailyAggregateSource source;
    private final SafetyScreeningUseCase outputScreening;
    private final InsightStore store;
    private final Clock clock;

    public NarrativeRewriter(ObjectProvider<InsightNarrator> narrator, UserProfileUseCase users,
                             DailyAggregateSource source, SafetyScreeningUseCase outputScreening,
                             InsightStore store, Clock clock) {
        this.narrator = narrator; this.users = users; this.source = source;
        this.outputScreening = outputScreening; this.store = store; this.clock = clock;
    }

    /** Accepted rewrites by draft key; drafts without an entry keep their template text. */
    public Map<String, Rewrite> rewrite(UUID userId, LocalDate from, LocalDate to, List<Draft> drafts) {
        InsightNarrator ai = narrator.getIfAvailable();
        if (ai == null || drafts.isEmpty() || !users.isConsentGranted(userId, "AI_PROCESSING")
                || source.hasElevatedRisk(userId, from, to)) return Map.of();
        InsightNarrator.Result result;
        try {
            result = ai.narrate(drafts.stream().map(d -> new InsightNarrator.Request(d.key(), d.type(),
                    d.direction(), d.facts(), d.text())).toList());
        } catch (RuntimeException e) {
            return Map.of();
        }
        store.recordNarrationUsage(new InsightStore.NarrationUsage(result.provider(), result.model(),
                result.inputTokens(), result.outputTokens(), result.estimatedCostUsd(), result.latencyMs(),
                result.succeeded()), clock.instant());
        Map<String, Rewrite> accepted = new HashMap<>();
        for (Draft draft : drafts) {
            String candidate = result.narratives().get(draft.key());
            boolean needsPlaceholder = draft.placeholderValue() != null;
            if (!InsightNarrativeValidator.isAcceptable(candidate, draft.text(), needsPlaceholder)) continue;
            String text = candidate.strip();
            if (needsPlaceholder) text = text.replace(InsightTemplates.TAG_PLACEHOLDER, draft.placeholderValue());
            if ("ALLOW".equals(outputScreening.screen(text).decision())) accepted.put(draft.key(), new Rewrite(text, result.model()));
        }
        return accepted;
    }
}
