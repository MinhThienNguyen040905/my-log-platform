package com.mylog.insight.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * When a thinking pattern becomes a card (THINKING_PATTERN_RECOGNITION_PLAN.md §5.2). A code needs at least
 * {@link #MIN_ENTRIES} different entries spread over {@link #MIN_WEEKS} ISO weeks with enough average
 * confidence, so one hard week is never presented as a habit. At most one new card a week and three live cards,
 * so the user feels helped rather than labelled. Pure functions; no Spring, no persistence.
 */
public final class ThinkingPatternRules {
    public static final int MIN_ENTRIES = 3;
    public static final int MIN_WEEKS = 2;
    public static final int MAX_QUOTES = 2;
    public static final int MAX_ACTIVE = 3;
    public static final Duration NEW_CARD_SPACING = Duration.ofDays(7);
    public static final Duration NOT_REALLY_QUIET = Duration.ofDays(60);
    public static final BigDecimal DEFAULT_MIN_CONFIDENCE = new BigDecimal("0.70");

    /** One stored signal of a current, readable entry inside the window. */
    public record SignalPoint(UUID signalId, UUID entryId, String code, LocalDate date, BigDecimal confidence) {}

    /** Per-code statistics; {@code quoteSignalIds} are the sentences to show (best first). */
    public record Stats(String code, int entryCount, int weekCount, BigDecimal averageConfidence, boolean eligible,
                        List<SignalPoint> evidence, List<UUID> quoteSignalIds) {}

    /** A card already stored for the user, in any status. */
    public record Existing(UUID id, String code, String status, String feedback, Instant feedbackAt,
                           boolean hasPlan, Instant createdAt) {}

    public record Update(UUID cardId, Stats stats) {}

    /** What a refresh does: refresh live cards, add at most one new card, retire the rest. */
    public record Plan(List<Update> updates, List<Stats> creates, List<UUID> supersedes) {}

    private ThinkingPatternRules() {}

    public static List<Stats> stats(List<SignalPoint> signals, Map<String, BigDecimal> minConfidence) {
        Map<String, Map<UUID, SignalPoint>> byCode = new LinkedHashMap<>();
        for (SignalPoint s : signals) {
            // One point per entry and code: the most confident one.
            byCode.computeIfAbsent(s.code(), k -> new LinkedHashMap<>())
                    .merge(s.entryId(), s, (a, b) -> b.confidence().compareTo(a.confidence()) > 0 ? b : a);
        }
        List<Stats> out = new ArrayList<>();
        byCode.forEach((code, perEntry) -> {
            List<SignalPoint> evidence = new ArrayList<>(perEntry.values());
            evidence.sort(Comparator.comparing(SignalPoint::date).thenComparing(SignalPoint::signalId));
            Set<String> weeks = new HashSet<>();
            BigDecimal sum = BigDecimal.ZERO;
            for (SignalPoint s : evidence) {
                weeks.add(s.date().get(IsoFields.WEEK_BASED_YEAR) + "-" + s.date().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
                sum = sum.add(s.confidence());
            }
            BigDecimal average = sum.divide(BigDecimal.valueOf(evidence.size()), 4, RoundingMode.HALF_UP);
            BigDecimal threshold = minConfidence.getOrDefault(code, DEFAULT_MIN_CONFIDENCE);
            boolean eligible = evidence.size() >= MIN_ENTRIES && weeks.size() >= MIN_WEEKS
                    && average.compareTo(threshold) >= 0;
            List<UUID> quotes = evidence.stream()
                    .sorted(Comparator.comparing(SignalPoint::confidence).reversed()
                            .thenComparing(SignalPoint::date, Comparator.reverseOrder()))
                    .limit(MAX_QUOTES).map(SignalPoint::signalId).toList();
            out.add(new Stats(code, evidence.size(), weeks.size(), average, eligible, List.copyOf(evidence), quotes));
        });
        out.sort(Comparator.comparing(Stats::entryCount).reversed()
                .thenComparing(Stats::averageConfidence, Comparator.reverseOrder()).thenComparing(Stats::code));
        return out;
    }

    public static Plan plan(List<Stats> stats, List<Existing> existing, Instant now) {
        Map<String, Stats> byCode = new HashMap<>();
        stats.forEach(s -> byCode.put(s.code(), s));
        Set<String> hidden = new HashSet<>();
        Set<String> quiet = new HashSet<>();
        Map<String, Existing> active = new HashMap<>();
        boolean recentNewCard = false;
        for (Existing card : existing) {
            if ("HIDDEN".equals(card.status())) hidden.add(card.code());
            if ("NOT_REALLY".equals(card.feedback()) && card.feedbackAt() != null
                    && card.feedbackAt().isAfter(now.minus(NOT_REALLY_QUIET))) quiet.add(card.code());
            if ("ACTIVE".equals(card.status())) active.put(card.code(), card);
            if (card.createdAt().isAfter(now.minus(NEW_CARD_SPACING))) recentNewCard = true;
        }

        List<Update> updates = new ArrayList<>();
        List<UUID> supersedes = new ArrayList<>();
        for (Existing card : active.values()) {
            Stats s = byCode.get(card.code());
            // A card with a running plan stays, so the user can look back at it; others leave when no longer supported.
            if (s != null && (s.eligible() || card.hasPlan())) updates.add(new Update(card.id(), s));
            else if (card.hasPlan()) updates.add(new Update(card.id(), empty(card.code())));
            else supersedes.add(card.id());
        }
        updates.sort(Comparator.comparing(u -> u.stats().code()));

        List<Stats> creates = new ArrayList<>();
        if (!recentNewCard && updates.size() < MAX_ACTIVE) {
            stats.stream().filter(Stats::eligible)
                    .filter(s -> !active.containsKey(s.code()) && !hidden.contains(s.code()) && !quiet.contains(s.code()))
                    .findFirst().ifPresent(creates::add);
        }
        return new Plan(List.copyOf(updates), List.copyOf(creates), List.copyOf(supersedes));
    }

    private static Stats empty(String code) {
        return new Stats(code, 0, 0, BigDecimal.ZERO.setScale(4), false, List.of(), List.of());
    }
}
