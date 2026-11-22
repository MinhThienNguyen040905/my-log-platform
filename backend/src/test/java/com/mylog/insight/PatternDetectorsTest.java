package com.mylog.insight;

import com.mylog.insight.application.query.ActivitySignal;
import com.mylog.insight.application.query.DailyMetric;
import com.mylog.insight.application.query.EntrySignal;
import com.mylog.insight.domain.ContrastDetectors;
import com.mylog.insight.domain.ExpectedPatterns;
import com.mylog.insight.domain.PatternDetectors;
import com.mylog.insight.domain.PatternFinding;
import com.mylog.insight.domain.PatternWindow;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
            double stress = date.getDayOfWeek() == DayOfWeek.WEDNESDAY ? 8.0 : 4.0;
            days.add(day(date, mood, stress, sleep));
        }
        List<PatternFinding> findings = PatternDetectors.findAll(window(days, List.of(), List.of()),
                PatternWindow.empty(FROM.minusDays(30), FROM.minusDays(1)), 7, 2);
        assertEquals(2, findings.size());
        assertTrue(findings.get(0).strength().compareTo(findings.get(1).strength()) >= 0);
    }

    @Test void findAllDropsAssociationsEveryoneExpects() {
        // Short nights go with lower mood and Mondays are stressful: both true, both obvious.
        List<DailyMetric> days = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            LocalDate date = FROM.plusDays(i);
            int sleep = i % 3 == 0 ? 300 : 450;
            double stress = date.getDayOfWeek() == DayOfWeek.MONDAY ? 8.0 : 4.0;
            days.add(day(date, sleep < 360 ? 4.5 : 7.0, stress, sleep));
        }
        assertNotNull(PatternDetectors.sleepMood(days, 7));
        assertNotNull(PatternDetectors.weekday(days));
        List<PatternFinding> findings = PatternDetectors.findAll(window(days, List.of(), List.of()),
                PatternWindow.empty(FROM.minusDays(30), FROM.minusDays(1)), 7, 5);
        assertTrue(findings.stream().noneMatch(f -> f.type().equals(PatternDetectors.SLEEP_MOOD)
                || f.type().equals(PatternDetectors.WEEKDAY)), findings.toString());
    }

    @Test void expectedPatternsKeepTheUnusualDirection() {
        assertTrue(ExpectedPatterns.isExpected(activity("meeting", "DOWN")));
        assertFalse(ExpectedPatterns.isExpected(activity("meeting", "UP")), "meetings going well is unusual");
        assertFalse(ExpectedPatterns.isExpected(activity("walking", "UP")), "personal resources stay visible");
        assertFalse(ExpectedPatterns.isExpected(new PatternFinding(PatternDetectors.WEEKDAY, "UP", BigDecimal.ONE, 20,
                Map.of("metric", "STRESS", "weekday", "WEDNESDAY"), List.of())));
        assertTrue(ExpectedPatterns.isExpected(new PatternFinding(PatternDetectors.WEEKDAY, "UP", BigDecimal.ONE, 20,
                Map.of("metric", "MOOD", "weekday", "SATURDAY"), List.of())));
    }

    @Test void exceptionDaysFindWhatHeavyDaysThatWentWellShared() {
        List<DailyMetric> days = new ArrayList<>();
        List<ActivitySignal> activities = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            LocalDate date = FROM.plusDays(i);
            boolean meeting = i % 3 == 0;               // 8 meeting days
            boolean walked = meeting && i % 2 == 0;     // 4 of them also had a walk
            if (meeting) activities.add(new ActivitySignal(date, "meeting"));
            if (walked) activities.add(new ActivitySignal(date, "walking"));
            days.add(day(date, meeting ? (walked ? 7.2 : 3.5) : 6.8, 5.0, 420));
        }
        PatternFinding finding = ContrastDetectors.exceptionDays(window(days, List.of(), activities));
        assertNotNull(finding);
        assertEquals("ACTIVITY", finding.facts().get("conditionKind"));
        assertEquals("meeting", finding.facts().get("conditionCode"));
        assertEquals(4, finding.facts().get("exceptionDays"));
        assertEquals("walking", finding.facts().get("featureCode"));
        assertEquals(4, finding.facts().get("featureDays"));
        assertEquals(8, finding.evidence().size());

        List<PatternFinding> all = PatternDetectors.findAll(window(days, List.of(), activities),
                PatternWindow.empty(FROM.minusDays(30), FROM.minusDays(1)), 7, 5);
        assertTrue(all.stream().anyMatch(f -> f.type().equals(ContrastDetectors.EXCEPTION)));
        assertTrue(all.stream().noneMatch(f -> f.type().equals(PatternDetectors.ACTIVITY_MOOD)
                && "meeting".equals(f.facts().get("activity"))), "the plain meeting association is covered");
    }

    @Test void exceptionDaysNeedLowDaysAndExceptionsBoth() {
        List<DailyMetric> days = new ArrayList<>();
        List<ActivitySignal> activities = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            boolean meeting = i % 3 == 0;
            if (meeting) activities.add(new ActivitySignal(FROM.plusDays(i), "meeting"));
            days.add(day(FROM.plusDays(i), meeting ? 3.5 : 6.8, 5.0, 420));
        }
        assertNull(ContrastDetectors.exceptionDays(window(days, List.of(), activities)), "no day went well");
    }

    @Test void conditionShiftComparesWithThePreviousWindow() {
        LocalDate prevFrom = FROM.minusDays(30);
        List<DailyMetric> before = new ArrayList<>(), now = new ArrayList<>();
        List<ActivitySignal> beforeActs = new ArrayList<>(), nowActs = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            boolean overtime = i % 4 == 0;
            if (overtime) {
                beforeActs.add(new ActivitySignal(prevFrom.plusDays(i), "work-overtime"));
                nowActs.add(new ActivitySignal(FROM.plusDays(i), "work-overtime"));
            }
            before.add(day(prevFrom.plusDays(i), overtime ? 4.0 : 6.8, 5.0, 420));
            now.add(day(FROM.plusDays(i), overtime ? 6.3 : 6.8, 5.0, 420));
        }
        PatternFinding finding = ContrastDetectors.conditionShift(window(now, List.of(), nowActs),
                new PatternWindow(prevFrom, FROM.minusDays(1), before, List.of(), beforeActs));
        assertNotNull(finding);
        assertEquals("UP", finding.direction(), "overtime days are no longer much lower");
        assertEquals("work-overtime", finding.facts().get("conditionCode"));
        assertEquals(new BigDecimal("6.3"), finding.facts().get("nowWith"));
        assertEquals(new BigDecimal("4.0"), finding.facts().get("prevWith"));
        assertNull(ContrastDetectors.conditionShift(window(now, List.of(), nowActs),
                PatternWindow.empty(prevFrom, FROM.minusDays(1))), "nothing to compare with");
    }

    @Test void topicShiftComparesShareOfEntries() {
        LocalDate prevFrom = FROM.minusDays(30);
        List<EntrySignal> before = new ArrayList<>(), now = new ArrayList<>();
        for (int i = 0; i < 18; i++) before.add(new EntrySignal(UUID.randomUUID(), prevFrom.plusDays(i), BigDecimal.valueOf(6),
                BigDecimal.valueOf(5), List.of("CALM"), List.of(i < 3 ? "RELATIONSHIPS" : i < 10 ? "WORK" : "HEALTH"), List.of()));
        for (int i = 0; i < 20; i++) now.add(entry(i, 6.0, List.of("CALM"),
                List.of(i < 9 ? "RELATIONSHIPS" : i < 16 ? "WORK" : "HEALTH"), List.of()));
        PatternFinding finding = ContrastDetectors.topicShift(window(List.of(), now, List.of()),
                new PatternWindow(prevFrom, FROM.minusDays(1), List.of(), before, List.of()));
        assertNotNull(finding);
        assertEquals("RELATIONSHIPS", finding.facts().get("topic"));
        assertEquals("UP", finding.direction());
        assertEquals(9L, finding.facts().get("nowCount"));
        assertEquals(3L, finding.facts().get("prevCount"));
        assertTrue(finding.evidence().stream().allMatch(p -> p.metric().equals("TOPIC_RELATIONSHIPS")));
    }

    @Test void recoveryShiftMeasuresDaysBackToAnOkayMood() {
        LocalDate prevFrom = FROM.minusDays(30);
        List<DailyMetric> before = new ArrayList<>(), now = new ArrayList<>();
        // Before: low days on 2 and 12, back to 6+ after 4 days. Now: back after 1 day.
        for (int i = 0; i < 30; i++) {
            int sinceLow = Math.min(i % 10 < 2 ? 99 : (i % 10) - 2, 99);
            before.add(day(prevFrom.plusDays(i), sinceLow == 0 ? 3.0 : sinceLow < 4 ? 5.0 : 6.8, 5.0, 420));
            now.add(day(FROM.plusDays(i), sinceLow == 0 ? 3.0 : 6.8, 5.0, 420));
        }
        PatternFinding finding = ContrastDetectors.recoveryShift(window(now, List.of(), List.of()),
                new PatternWindow(prevFrom, FROM.minusDays(1), before, List.of(), List.of()));
        assertNotNull(finding);
        assertEquals("UP", finding.direction(), "faster recovery");
        assertEquals(new BigDecimal("1.0"), finding.facts().get("nowDays"));
        assertEquals(new BigDecimal("4.0"), finding.facts().get("prevDays"));
    }

    static PatternWindow window(List<DailyMetric> days, List<EntrySignal> entries, List<ActivitySignal> activities) {
        return new PatternWindow(FROM, TO, days, entries, activities);
    }

    static PatternFinding activity(String code, String direction) {
        return new PatternFinding(PatternDetectors.ACTIVITY_MOOD, direction, BigDecimal.ONE, 20,
                Map.of("activity", code), List.of());
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
