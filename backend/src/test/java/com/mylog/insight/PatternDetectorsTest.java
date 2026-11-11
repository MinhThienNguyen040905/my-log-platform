package com.mylog.insight;

import com.mylog.insight.application.query.ActivitySignal;
import com.mylog.insight.application.query.DailyMetric;
import com.mylog.insight.application.query.EntrySignal;
import com.mylog.insight.domain.PatternDetectors;
import com.mylog.insight.domain.PatternFinding;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Synthetic 30-day series only. */
class PatternDetectorsTest {
    static final LocalDate FROM = LocalDate.of(2026, 10, 13); // a Tuesday
    static final LocalDate TO = FROM.plusDays(29);

    @Test void weekdayFindsHigherStressOnMondays() {
        List<DailyMetric> days = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            LocalDate date = FROM.plusDays(i);
            double stress = date.getDayOfWeek() == DayOfWeek.MONDAY ? 7.5 : 4.0 + (i % 3) * 0.2;
            days.add(day(date, 6.0, stress, 420));
        }
        PatternFinding finding = PatternDetectors.weekday(days);
        assertNotNull(finding);
        assertEquals("UP", finding.direction());
        assertEquals("STRESS", finding.facts().get("metric"));
        assertEquals("MONDAY", finding.facts().get("weekday"));
        assertTrue(finding.evidence().stream().allMatch(p -> p.date().getDayOfWeek() == DayOfWeek.MONDAY));
    }

    @Test void weekdayIgnoresFlatOrTooShortSeries() {
        List<DailyMetric> flat = new ArrayList<>();
        for (int i = 0; i < 30; i++) flat.add(day(FROM.plusDays(i), 6.0 + (i % 2) * 0.3, 5.0 + (i % 3) * 0.3, 420));
        assertNull(PatternDetectors.weekday(flat));
        assertNull(PatternDetectors.weekday(flat.subList(0, 10)));
    }

    @Test void trendShiftComparesHalvesOfTheWindow() {
        List<DailyMetric> days = new ArrayList<>();
        for (int i = 0; i < 30; i++) days.add(day(FROM.plusDays(i), i < 15 ? 7.0 : 5.2, 5.0, 420));
        PatternFinding finding = PatternDetectors.trendShift(days, FROM, TO);
        assertNotNull(finding);
        assertEquals("DOWN", finding.direction());
        assertEquals(new BigDecimal("7.0"), finding.facts().get("firstAverage"));
        assertEquals(new BigDecimal("5.2"), finding.facts().get("secondAverage"));
        assertNull(PatternDetectors.trendShift(days.subList(0, 15), FROM, TO), "second half has no samples");
    }

    @Test void emotionTopicNeedsLiftAndEnoughCooccurrences() {
        List<EntrySignal> entries = new ArrayList<>();
        for (int i = 0; i < 8; i++) entries.add(entry(i, 5.0, List.of("ANXIETY"), List.of("WORK"), List.of()));
        for (int i = 8; i < 20; i++) entries.add(entry(i, 7.0, List.of("CALM"), List.of("SELF_CARE"), List.of()));
        PatternFinding finding = PatternDetectors.emotionTopic(entries);
        assertNotNull(finding);
        assertEquals("ANXIETY", finding.facts().get("emotion"));
        assertEquals("WORK", finding.facts().get("topic"));
        assertEquals(100L, finding.facts().get("percentWithTopic"));
        assertEquals(40L, finding.facts().get("percentOverall"));
        assertNull(PatternDetectors.emotionTopic(entries.subList(0, 9)), "fewer than 10 analyzed entries");
    }

    @Test void emotionTopicSkipsUnanalyzedEntriesAndUniformData() {
        List<EntrySignal> entries = new ArrayList<>();
        for (int i = 0; i < 20; i++) entries.add(entry(i, 6.0, List.of("CALM"), List.of(i % 2 == 0 ? "WORK" : "HEALTH"), List.of()));
        for (int i = 20; i < 30; i++) entries.add(entry(i, 6.0, List.of(), List.of(), List.of()));
        assertNull(PatternDetectors.emotionTopic(entries));
    }

    @Test void tagMoodComparesEntriesWithAndWithoutTag() {
        UUID friends = UUID.randomUUID();
        List<EntrySignal> entries = new ArrayList<>();
        for (int i = 0; i < 6; i++) entries.add(entry(i, 8.0, List.of(), List.of(), List.of(friends)));
        for (int i = 6; i < 14; i++) entries.add(entry(i, 5.5, List.of(), List.of(), List.of()));
        PatternFinding finding = PatternDetectors.tagMood(entries);
        assertNotNull(finding);
        assertEquals("UP", finding.direction());
        assertEquals(friends.toString(), finding.facts().get("tagId"));
        assertFalse(finding.facts().containsValue("Bạn bè"), "tag names are never stored in facts");
    }

    @Test void activityMoodUsesCheckinActivities() {
        List<DailyMetric> days = new ArrayList<>();
        List<ActivitySignal> activities = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            boolean walked = i % 2 == 0;
            days.add(day(FROM.plusDays(i), walked ? 7.4 : 5.6, 5.0, 420));
            if (walked) activities.add(new ActivitySignal(FROM.plusDays(i), "walking"));
        }
        PatternFinding finding = PatternDetectors.activityMood(days, activities);
        assertNotNull(finding);
        assertEquals("walking", finding.facts().get("activity"));
        assertEquals("UP", finding.direction());
    }

    @Test void findAllKeepsStrongestFindingsWithinLimit() {
        List<DailyMetric> days = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            LocalDate date = FROM.plusDays(i);
            int sleep = i % 3 == 0 ? 300 : 450;
            double mood = (sleep < 360 ? 4.5 : 7.0) + (i < 15 ? 0.8 : -0.8);
            double stress = date.getDayOfWeek() == DayOfWeek.MONDAY ? 8.0 : 4.0;
            days.add(day(date, mood, stress, sleep));
        }
        List<PatternFinding> findings = PatternDetectors.findAll(days, List.of(), List.of(), FROM, TO, 7, 2);
        assertEquals(2, findings.size());
        assertTrue(findings.get(0).strength().compareTo(findings.get(1).strength()) >= 0);
    }

    static DailyMetric day(LocalDate date, double mood, double stress, int sleep) {
        return new DailyMetric(date, "CHECKIN", UUID.randomUUID(), 1, BigDecimal.valueOf(mood),
                BigDecimal.valueOf(stress), BigDecimal.valueOf(5), sleep);
    }

    static EntrySignal entry(int i, double mood, List<String> emotions, List<String> topics, List<UUID> tags) {
        return new EntrySignal(UUID.randomUUID(), FROM.plusDays(i), BigDecimal.valueOf(mood), BigDecimal.valueOf(5),
                emotions, topics, tags);
    }
}
