package com.mylog.insight.application;

import com.mylog.insight.application.query.InsightPage;
import com.mylog.insight.domain.MoodSleepAssociation;
import com.mylog.insight.domain.PatternDetectors;
import com.mylog.insight.domain.PatternFinding;
import com.mylog.journal.application.JournalTagService;
import com.mylog.journal.application.query.JournalTagView;
import com.mylog.platform.web.InvalidRequestException;
import com.mylog.platform.web.ResourceNotFoundException;
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
    private final NarrativeRewriter rewriter;

    public InsightService(DailyAggregateSource source, InsightStore store, Clock clock,
                          @Value("${mylog.insights.minimum-samples:7}") int minimumSamples,
                          JournalTagService tags, NarrativeRewriter rewriter) {
        if (minimumSamples < 3 || minimumSamples > 90) throw new IllegalArgumentException("Invalid insight sample threshold");
        this.source = source; this.store = store; this.clock = clock; this.minimumSamples = minimumSamples;
        this.tags = tags; this.rewriter = rewriter;
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
     * provider may only reword the template sentence (see {@link NarrativeRewriter}). Deliberately not
     * transactional: the optional provider call must not run inside a database transaction.
     */
    public InsightPage refreshRecent(UUID userId) {
        return refreshRecent(userId, true);
    }

    /**
     * Scheduled refresh: for users whose local time is {@code localHour}, recompute once per local day and only
     * when journal/check-in data changed since the last refresh. {@code allowAi=false} keeps template wording so
     * the nightly run does not call a provider for every user. Returns how many users were refreshed.
     */
    public int refreshDueUsers(int localHour, boolean allowAi) {
        int refreshed = 0;
        UUID cursor = null;
        while (true) {
            var page = store.activeUsersAfter(cursor, 500);
            for (var user : page) {
                var now = clock.instant().atZone(ZoneId.of(user.timezone()));
                if (now.getHour() != localHour) continue;
                var startOfDay = now.toLocalDate().atStartOfDay(now.getZone()).toInstant();
                if (!store.needsPatternRefresh(user.userId(), PATTERN_VERSION, startOfDay)) continue;
                try {
                    refreshRecent(user.userId(), allowAi);
                    refreshed++;
                } catch (RuntimeException ignored) {
                    // One user's failure must not stop the run; the next run retries.
                }
            }
            if (page.size() < 500) return refreshed;
            cursor = page.getLast().userId();
        }
    }

    InsightPage refreshRecent(UUID userId, boolean allowAi) {
        String timezone = source.timezone(userId);
        LocalDate to = LocalDate.now(clock.withZone(ZoneId.of(timezone)));
        LocalDate from = to.minusDays(PATTERN_WINDOW_DAYS - 1L);
        List<PatternFinding> findings = PatternDetectors.findAll(source.daily(userId, from, to),
                source.entrySignals(userId, from, to), source.activities(userId, from, to),
                from, to, minimumSamples, MAX_PATTERNS);

        Map<String, String> tagNames = new HashMap<>();
        if (findings.stream().anyMatch(f -> f.facts().containsKey("tagId")))
            for (JournalTagView tag : tags.list(userId)) tagNames.put(tag.id().toString(), tag.name());

        List<NarrativeRewriter.Draft> drafts = new ArrayList<>();
        for (int i = 0; i < findings.size(); i++) {
            PatternFinding finding = findings.get(i);
            // A falling mood trend keeps the reviewed template wording.
            if (PatternDetectors.TREND.equals(finding.type()) && "DOWN".equals(finding.direction())) continue;
            String tagName = finding.facts().containsKey("tagId")
                    ? tagNames.getOrDefault(String.valueOf(finding.facts().get("tagId")), "") : null;
            drafts.add(new NarrativeRewriter.Draft("i" + (i + 1), finding.type(), finding.direction(),
                    InsightTemplates.readableFacts(finding),
                    InsightTemplates.render(finding, InsightTemplates.TAG_PLACEHOLDER), tagName));
        }
        Map<String, NarrativeRewriter.Rewrite> rewrites = findings.isEmpty() || !allowAi ? Map.of()
                : rewriter.rewrite(userId, from, to, drafts);

        List<InsightStore.PatternInsight> insights = new ArrayList<>();
        for (int i = 0; i < findings.size(); i++) {
            PatternFinding finding = findings.get(i);
            var rewrite = rewrites.get("i" + (i + 1));
            String template = InsightTemplates.render(finding,
                    tagNames.getOrDefault(String.valueOf(finding.facts().get("tagId")), ""));
            insights.add(rewrite == null
                    ? new InsightStore.PatternInsight(finding, template, "STATISTICAL", null)
                    : new InsightStore.PatternInsight(finding, rewrite.text(), "AI_ASSISTED", rewrite.model()));
        }
        store.replacePatterns(userId, from, to, timezone, PATTERN_VERSION, insights, clock.instant());
        return store.list(userId, from, to, null, 20);
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
