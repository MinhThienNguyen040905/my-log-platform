package com.mylog.insight.application;

import com.mylog.analysis.application.ThinkingPatternTaxonomy;
import com.mylog.insight.application.query.ThinkingPatternView;
import com.mylog.insight.domain.ThinkingPatternRules;
import com.mylog.platform.web.ConflictException;
import com.mylog.platform.web.InvalidRequestException;
import com.mylog.platform.web.ResourceNotFoundException;
import com.mylog.selfcare.application.SelfCareService;
import com.mylog.user.application.UserProfileUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Thinking-pattern cards over the last 30 days (THINKING_PATTERN_RECOGNITION_PLAN.md §5). Reads stored signals
 * only and never calls a provider: deterministic rules decide every card and its numbers, and the wording is
 * the template. Shown only while the user's THINKING_PATTERNS opt-in is on; a window containing an entry
 * screened HIGH/CRITICAL shows no cards.
 */
@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class ThinkingPatternService {
    public static final int WINDOW_DAYS = 30;
    static final String CONSENT = "THINKING_PATTERNS";
    static final Set<String> REASONS = Set.of("NOT_LIKE_ME", "DONT_WANT_TO_SEE", "WRONG_QUOTE");

    public record PlanStarted(UUID goalId, boolean created) {}

    private final ThinkingPatternStore store;
    private final DailyAggregateSource source;
    private final UserProfileUseCase users;
    private final SelfCareService selfCare;
    private final Clock clock;

    public ThinkingPatternService(ThinkingPatternStore store, DailyAggregateSource source, UserProfileUseCase users,
                                  SelfCareService selfCare, Clock clock) {
        this.store = store; this.source = source; this.users = users; this.selfCare = selfCare; this.clock = clock;
    }

    @Transactional
    public List<ThinkingPatternView> refresh(UUID userId) {
        return refreshCards(userId).stream().map(ThinkingPatternService::view).toList();
    }

    private List<ThinkingPatternStore.Card> refreshCards(UUID userId) {
        if (!users.isConsentGranted(userId, CONSENT)) return List.of();
        store.lock(userId);
        String timezone = source.timezone(userId);
        LocalDate to = LocalDate.now(clock.withZone(ZoneId.of(timezone)));
        LocalDate from = to.minusDays(WINDOW_DAYS - 1L);
        Instant now = clock.instant();
        var existing = store.existing(userId, ThinkingPatternTaxonomy.VERSION);

        if (source.hasElevatedRisk(userId, from, to)) {
            store.supersede(userId, existing.stream().filter(c -> "ACTIVE".equals(c.status()))
                    .map(ThinkingPatternRules.Existing::id).toList(), now);
            return List.of();
        }
        var stats = ThinkingPatternRules.stats(store.signals(userId, from, to, ThinkingPatternTaxonomy.VERSION), Map.of());
        var plan = ThinkingPatternRules.plan(stats, existing, now);
        store.supersede(userId, plan.supersedes(), now);
        for (var update : plan.updates())
            store.update(userId, update.cardId(), write(update.stats(), from, to, timezone), now);
        for (var create : plan.creates())
            store.create(userId, ThinkingPatternTaxonomy.VERSION, write(create, from, to, timezone), now);
        return store.active(userId);
    }

    @Transactional(readOnly = true)
    public List<ThinkingPatternView> list(UUID userId) {
        if (!users.isConsentGranted(userId, CONSENT)) return List.of();
        return store.active(userId).stream().map(ThinkingPatternService::view).toList();
    }

    static ThinkingPatternView view(ThinkingPatternStore.Card card) {
        var text = ThinkingPatternTexts.of(card.code());
        return new ThinkingPatternView(card.id(), card.code(), text.name(), card.narrative(), text.explanation(),
                ThinkingPatternTexts.steps(card.code()),
                card.quotes().stream().map(q -> new ThinkingPatternView.Quote(q.text(), q.date())).toList(),
                card.entryCount(), card.weekCount(), card.periodStart(), card.periodEnd(), card.feedback(),
                card.planGoalId(), card.planStartedAt(), card.generatedBy(), card.knowledgeVersionId(),
                card.knowledgeVersionId() != null, card.createdAt(), card.updatedAt());
    }

    @Transactional
    public void feedback(UUID userId, UUID cardId, String feedback, String reason) {
        if (!Set.of("CONFIRMED", "NOT_REALLY").contains(feedback)
                || reason != null && (!"NOT_REALLY".equals(feedback) || !REASONS.contains(reason)))
            throw new InvalidRequestException();
        if (!store.feedback(userId, cardId, feedback, reason, clock.instant())) throw notFound();
    }

    @Transactional
    public void hide(UUID userId, UUID cardId) {
        if (!store.hide(userId, cardId, clock.instant())) throw notFound();
    }

    /** Creates a three-week goal with a daily habit for the first step; calling again returns the same goal. */
    @Transactional
    public PlanStarted startPlan(UUID userId, UUID cardId) {
        var card = store.find(userId, cardId).filter(c -> "ACTIVE".equals(c.status())).orElseThrow(this::notFound);
        if (card.planGoalId() != null) return new PlanStarted(card.planGoalId(), false);
        var text = ThinkingPatternTexts.of(card.code());
        var steps = ThinkingPatternTexts.steps(card.code());
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(source.timezone(userId))));
        var goal = selfCare.createGoal(userId, "MINDFULNESS", "Thử từng bước: " + text.name(),
                // One line: self-care text rejects control characters, including line breaks.
                "Tuần 1: " + steps.get(0) + " Tuần 2: " + steps.get(1) + " Tuần 3: " + steps.get(2),
                today, today.plusDays(20));
        selfCare.createHabit(userId, goal.id(), "Ghi lại khi nhận ra mình đang " + text.name(),
                BigDecimal.ONE, "times", "DAILY", List.of());
        if (!store.attachPlan(userId, cardId, goal.id(), clock.instant()))
            throw new ConflictException("Kế hoạch đã được tạo ở nơi khác.");
        return new PlanStarted(goal.id(), true);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> exportAll(UUID userId) {
        return store.exportAll(userId);
    }

    private ThinkingPatternStore.Write write(ThinkingPatternRules.Stats stats, LocalDate from, LocalDate to, String timezone) {
        return new ThinkingPatternStore.Write(stats.code(), from, to, timezone, stats,
                ThinkingPatternTexts.narrative(stats.code(), stats.entryCount(), stats.weekCount()),
                knowledgeVersion(stats.code()));
    }

    /**
     * The reviewed article TP_&lt;CODE&gt; when one is approved; cards work without it. Read straight from the
     * catalog, never through semantic retrieval, so no embedding provider is called inside this transaction.
     */
    private UUID knowledgeVersion(String code) {
        return store.approvedKnowledgeVersion("TP_" + code, "vi", clock.instant()).orElse(null);
    }

    private ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("Không tìm thấy thẻ.");
    }
}
