package com.mylog.insight.application;

import com.mylog.insight.application.query.InsightPage;
import com.mylog.insight.domain.MoodSleepAssociation;
import com.mylog.insight.domain.PatternDetectors;
import com.mylog.insight.domain.PatternFinding;
import com.mylog.journal.application.JournalTagService;
import com.mylog.journal.application.query.JournalTagView;
import com.mylog.platform.web.InvalidRequestException;
import com.mylog.platform.web.ResourceNotFoundException;
import com.mylog.safety.application.SafetyScreeningUseCase;
import com.mylog.user.application.UserProfileUseCase;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class InsightService {
    /** Pattern insights look at the last 30 local days only. */
    public static final int PATTERN_WINDOW_DAYS = 30;
    public static final String PATTERN_VERSION = "pattern-30d-v1";
    static final int MAX_PATTERNS = 5;

    private final DailyAggregateSource source;
    private final InsightStore store;
    private final Clock clock;
    private final int minimumSamples;
    private final JournalTagService tags;
    private final UserProfileUseCase users;
    private final SafetyScreeningUseCase outputScreening;
    private final ObjectProvider<InsightNarrator> narrator;

    public InsightService(DailyAggregateSource source, InsightStore store, Clock clock,
                          @Value("${mylog.insights.minimum-samples:7}") int minimumSamples,
                          JournalTagService tags, UserProfileUseCase users,
                          SafetyScreeningUseCase outputScreening, ObjectProvider<InsightNarrator> narrator) {
        if (minimumSamples < 3 || minimumSamples > 90) throw new IllegalArgumentException("Invalid insight sample threshold");
        this.source = source; this.store = store; this.clock = clock; this.minimumSamples = minimumSamples;
        this.tags = tags; this.users = users; this.outputScreening = outputScreening; this.narrator = narrator;
    }

    @Transactional
    public void generate(UUID userId, LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to) || to.isAfter(from.plusDays(89)))
            throw new InvalidRequestException();
        String timezone = source.timezone(userId);
        var association = MoodSleepAssociation.calculate(source.daily(userId, from, to), minimumSamples);
        if (association != null) store.saveAssociation(userId, from, to, timezone, association, clock.instant());
    }

    /**
     * Recomputes pattern insights for the last 30 days. Statistics decide every insight; an approved AI
     * provider may only reword the template sentence, and only with AI_PROCESSING consent. Deliberately not
     * transactional: the optional provider call must not run inside a database transaction.
     */
    public InsightPage refreshRecent(UUID userId) {
        String timezone = source.timezone(userId);
        LocalDate to = LocalDate.now(clock.withZone(ZoneId.of(timezone)));
        LocalDate from = to.minusDays(PATTERN_WINDOW_DAYS - 1L);
        List<PatternFinding> findings = PatternDetectors.findAll(source.daily(userId, from, to),
                source.entrySignals(userId, from, to), source.activities(userId, from, to),
                from, to, minimumSamples, MAX_PATTERNS);

        Map<String, String> tagNames = new HashMap<>();
        if (findings.stream().anyMatch(f -> f.facts().containsKey("tagId")))
            for (JournalTagView tag : tags.list(userId)) tagNames.put(tag.id().toString(), tag.name());

        List<InsightStore.PatternInsight> insights = new ArrayList<>();
        for (PatternFinding finding : findings) {
            String tagName = tagNames.getOrDefault(String.valueOf(finding.facts().get("tagId")), "");
            insights.add(new InsightStore.PatternInsight(finding, InsightTemplates.render(finding, tagName),
                    "STATISTICAL", null));
        }
        if (!insights.isEmpty()) insights = reword(userId, from, to, insights, tagNames);
        store.replacePatterns(userId, from, to, timezone, PATTERN_VERSION, insights, clock.instant());
        return store.list(userId, from, to, null, 20);
    }

    private List<InsightStore.PatternInsight> reword(UUID userId, LocalDate from, LocalDate to,
                                                     List<InsightStore.PatternInsight> insights,
                                                     Map<String, String> tagNames) {
        InsightNarrator ai = narrator.getIfAvailable();
        if (ai == null || !users.isConsentGranted(userId, "AI_PROCESSING") || source.hasElevatedRisk(userId, from, to))
            return insights;
        List<InsightNarrator.Request> requests = new ArrayList<>();
        Map<String, String> drafts = new HashMap<>();
        for (int i = 0; i < insights.size(); i++) {
            PatternFinding finding = insights.get(i).finding();
            // A falling mood trend keeps the reviewed template wording.
            if (PatternDetectors.TREND.equals(finding.type()) && "DOWN".equals(finding.direction())) continue;
            String draft = InsightTemplates.render(finding, InsightTemplates.TAG_PLACEHOLDER);
            String key = "i" + (i + 1);
            drafts.put(key, draft);
            requests.add(new InsightNarrator.Request(key, finding.type(), finding.direction(),
                    InsightTemplates.readableFacts(finding), draft));
        }
        if (requests.isEmpty()) return insights;
        InsightNarrator.Result result;
        try {
            result = ai.narrate(requests);
        } catch (RuntimeException e) {
            return insights;
        }
        store.recordNarrationUsage(new InsightStore.NarrationUsage(result.provider(), result.model(),
                result.inputTokens(), result.outputTokens(), result.estimatedCostUsd(), result.latencyMs(),
                result.succeeded()), clock.instant());
        List<InsightStore.PatternInsight> out = new ArrayList<>(insights.size());
        for (int i = 0; i < insights.size(); i++) {
            var insight = insights.get(i);
            String key = "i" + (i + 1);
            String candidate = result.narratives().get(key);
            boolean needsTag = insight.finding().facts().containsKey("tagId");
            if (drafts.containsKey(key) && InsightNarrativeValidator.isAcceptable(candidate, drafts.get(key), needsTag)) {
                String text = candidate.strip().replace(InsightTemplates.TAG_PLACEHOLDER,
                        tagNames.getOrDefault(String.valueOf(insight.finding().facts().get("tagId")), ""));
                if ("ALLOW".equals(outputScreening.screen(text).decision())) {
                    out.add(new InsightStore.PatternInsight(insight.finding(), text, "AI_ASSISTED", result.model()));
                    continue;
                }
            }
            out.add(insight);
        }
        return out;
    }

    @Transactional
    public void hide(UUID userId, UUID insightId) {
        if (!store.hide(userId, insightId)) throw new ResourceNotFoundException("Không tìm thấy nhận định.");
    }

    @Transactional(readOnly = true)
    public InsightPage list(UUID userId, LocalDate from, LocalDate to, String cursor, int limit) {
        if (limit < 1 || limit > 50 || from != null && to != null && from.isAfter(to))
            throw new InvalidRequestException();
        return store.list(userId, from, to, cursor, limit);
    }
}
