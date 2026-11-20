package com.mylog.insight;

import com.mylog.insight.domain.ThinkingPatternRules;
import com.mylog.insight.domain.ThinkingPatternRules.Existing;
import com.mylog.insight.domain.ThinkingPatternRules.SignalPoint;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThinkingPatternRulesTest {
    // Thursday 19 Nov 2026 (ISO week 47). Mondays: 16 Nov (W47), 9 Nov (W46), 2 Nov (W45).
    static final LocalDate TODAY = LocalDate.of(2026, 11, 19);
    static final Instant NOW = Instant.parse("2026-11-19T10:00:00Z");

    @Test void needsThreeEntriesOverTwoWeeksWithEnoughConfidence() {
        var stats = byCode(ThinkingPatternRules.stats(List.of(
                point("CATASTROPHIZING", TODAY, "0.9"), point("CATASTROPHIZING", TODAY.minusDays(8), "0.8"),
                point("CATASTROPHIZING", TODAY.minusDays(15), "0.8"),
                // three entries, all inside ISO week 46
                point("MIND_READING", LocalDate.of(2026, 11, 9), "0.9"), point("MIND_READING", LocalDate.of(2026, 11, 10), "0.9"),
                point("MIND_READING", LocalDate.of(2026, 11, 11), "0.9"),
                point("SHOULD_STATEMENTS", TODAY, "0.9"), point("SHOULD_STATEMENTS", TODAY.minusDays(14), "0.9"),
                point("RUMINATION", TODAY, "0.6"), point("RUMINATION", TODAY.minusDays(8), "0.6"),
                point("RUMINATION", TODAY.minusDays(15), "0.6")), Map.of()));

        assertTrue(stats.get("CATASTROPHIZING").eligible());
        assertEquals(3, stats.get("CATASTROPHIZING").weekCount());
        assertFalse(stats.get("MIND_READING").eligible(), "one hard week is not a habit");
        assertEquals(1, stats.get("MIND_READING").weekCount());
        assertFalse(stats.get("SHOULD_STATEMENTS").eligible(), "two entries are not enough");
        assertFalse(stats.get("RUMINATION").eligible(), "average confidence below 0.70");
        assertTrue(ThinkingPatternRules.stats(List.of(point("RUMINATION", TODAY, "0.6"),
                point("RUMINATION", TODAY.minusDays(8), "0.6"), point("RUMINATION", TODAY.minusDays(15), "0.6")),
                Map.of("RUMINATION", new BigDecimal("0.55"))).getFirst().eligible(), "per-code threshold");
    }

    @Test void countsAnEntryOnceAndQuotesTheTwoMostConfidentSentences() {
        UUID entry = UUID.randomUUID();
        var best = point("WORRY_LOOP", TODAY.minusDays(1), "0.95");
        var second = point("WORRY_LOOP", TODAY.minusDays(9), "0.85");
        var stats = ThinkingPatternRules.stats(List.of(
                new SignalPoint(UUID.randomUUID(), entry, "WORRY_LOOP", TODAY, new BigDecimal("0.70")),
                new SignalPoint(UUID.randomUUID(), entry, "WORRY_LOOP", TODAY, new BigDecimal("0.75")),
                best, second, point("WORRY_LOOP", TODAY.minusDays(16), "0.80")), Map.of()).getFirst();
        assertEquals(4, stats.entryCount());
        assertEquals(List.of(best.signalId(), second.signalId()), stats.quoteSignalIds());
        assertEquals(new BigDecimal("0.8375"), stats.averageConfidence());
    }

    @Test void addsAtMostOneNewCardAWeekAndNeverMoreThanThreeLiveCards() {
        var stats = ThinkingPatternRules.stats(eligible("CATASTROPHIZING", "RUMINATION", "AVOIDANCE", "PERFECTIONISM"), Map.of());

        var first = ThinkingPatternRules.plan(stats, List.of(), NOW);
        assertEquals(1, first.creates().size(), "one new card per refresh");

        var recent = List.of(card("CATASTROPHIZING", "ACTIVE", null, null, false, NOW.minus(2, ChronoUnit.DAYS)));
        var plan = ThinkingPatternRules.plan(stats, recent, NOW);
        assertEquals(0, plan.creates().size(), "a card was added less than a week ago");
        assertEquals(1, plan.updates().size(), "the live card is refreshed");

        var full = List.of(card("CATASTROPHIZING", "ACTIVE", null, null, false, NOW.minus(30, ChronoUnit.DAYS)),
                card("RUMINATION", "ACTIVE", null, null, false, NOW.minus(20, ChronoUnit.DAYS)),
                card("AVOIDANCE", "ACTIVE", null, null, false, NOW.minus(10, ChronoUnit.DAYS)));
        assertEquals(0, ThinkingPatternRules.plan(stats, full, NOW).creates().size(), "three live cards already");
    }

    @Test void respectsHiddenCodesAndASixtyDayQuietTimeAfterNotReally() {
        var stats = ThinkingPatternRules.stats(eligible("CATASTROPHIZING", "RUMINATION"), Map.of());
        var hidden = card("CATASTROPHIZING", "HIDDEN", null, null, false, NOW.minus(90, ChronoUnit.DAYS));
        var notReally = card("RUMINATION", "SUPERSEDED", "NOT_REALLY", NOW.minus(20, ChronoUnit.DAYS), false,
                NOW.minus(40, ChronoUnit.DAYS));
        assertEquals(0, ThinkingPatternRules.plan(stats, List.of(hidden, notReally), NOW).creates().size());

        var oldNotReally = card("RUMINATION", "SUPERSEDED", "NOT_REALLY", NOW.minus(61, ChronoUnit.DAYS), false,
                NOW.minus(80, ChronoUnit.DAYS));
        var plan = ThinkingPatternRules.plan(stats, List.of(hidden, oldNotReally), NOW);
        assertEquals(List.of("RUMINATION"), plan.creates().stream().map(ThinkingPatternRules.Stats::code).toList());
    }

    @Test void retiresUnsupportedCardsUnlessAPlanIsRunning() {
        var stats = ThinkingPatternRules.stats(List.of(point("AVOIDANCE", TODAY, "0.9")), Map.of());
        var plain = card("RUMINATION", "ACTIVE", null, null, false, NOW.minus(30, ChronoUnit.DAYS));
        var withPlan = card("PERFECTIONISM", "ACTIVE", "CONFIRMED", NOW.minus(25, ChronoUnit.DAYS), true,
                NOW.minus(30, ChronoUnit.DAYS));
        var weak = card("AVOIDANCE", "ACTIVE", null, null, false, NOW.minus(30, ChronoUnit.DAYS));
        var plan = ThinkingPatternRules.plan(stats, List.of(plain, withPlan, weak), NOW);
        assertEquals(List.of(plain.id(), weak.id()).stream().sorted().toList(), plan.supersedes().stream().sorted().toList());
        assertEquals(List.of(withPlan.id()), plan.updates().stream().map(ThinkingPatternRules.Update::cardId).toList());
        assertEquals(0, plan.updates().getFirst().stats().entryCount());
    }

    private static List<SignalPoint> eligible(String... codes) {
        List<SignalPoint> out = new ArrayList<>();
        int extra = codes.length;
        for (String code : codes) {
            for (int d : new int[] {1, 8, 15}) out.add(point(code, TODAY.minusDays(d), "0.8"));
            // Earlier codes get more entries, so they are the strongest.
            for (int i = 0; i < extra; i++) out.add(point(code, TODAY.minusDays(20 + i), "0.8"));
            extra--;
        }
        return out;
    }

    private static SignalPoint point(String code, LocalDate date, String confidence) {
        return new SignalPoint(UUID.randomUUID(), UUID.randomUUID(), code, date, new BigDecimal(confidence));
    }

    private static Existing card(String code, String status, String feedback, Instant feedbackAt, boolean plan, Instant created) {
        return new Existing(UUID.randomUUID(), code, status, feedback, feedbackAt, plan, created);
    }

    private static Map<String, ThinkingPatternRules.Stats> byCode(List<ThinkingPatternRules.Stats> stats) {
        Map<String, ThinkingPatternRules.Stats> out = new java.util.HashMap<>();
        stats.forEach(s -> out.put(s.code(), s));
        return out;
    }
}
