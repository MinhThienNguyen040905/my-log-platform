package com.mylog.insight.domain;

import com.mylog.insight.application.query.ActivitySignal;
import com.mylog.insight.application.query.DailyMetric;
import com.mylog.insight.application.query.EntrySignal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;

/**
 * Deterministic pattern detectors over a short window (the last 30 days). Each returns {@code null} when
 * the sample is too small or the effect too weak, so noise does not become an insight. Results describe
 * observed co-occurrence only, never causation.
 */
public final class PatternDetectors {
    public static final String WEEKDAY = "WEEKDAY_PATTERN";
    public static final String TREND = "MOOD_TREND_SHIFT";
    public static final String EMOTION_TOPIC = "EMOTION_TOPIC_COOCCURRENCE";
    public static final String TAG_MOOD = "TAG_MOOD_ASSOCIATION";
    public static final String ACTIVITY_MOOD = "ACTIVITY_MOOD_ASSOCIATION";
    public static final String SLEEP_MOOD = "SLEEP_MOOD_ASSOCIATION";

    static final double WEEKDAY_MIN_DIFF = 1.0;
    static final double TREND_MIN_DIFF = 1.0;
    static final double GROUP_MIN_DIFF = 0.8;
    static final double MIN_LIFT = 1.5;
    static final int SHORT_SLEEP_MINUTES = 360;

    private PatternDetectors() {}

    /** Runs every detector and keeps the strongest {@code limit} findings. */
    public static List<PatternFinding> findAll(List<DailyMetric> days, List<EntrySignal> entries,
                                               List<ActivitySignal> activities, LocalDate from, LocalDate to,
                                               int minimumSamples, int limit) {
        List<PatternFinding> all = new ArrayList<>();
        for (PatternFinding finding : new PatternFinding[] {
                sleepMood(days, minimumSamples), weekday(days), trendShift(days, from, to),
                emotionTopic(entries), tagMood(entries), activityMood(days, activities)}) {
            if (finding != null) all.add(finding);
        }
        all.sort(Comparator.comparing(PatternFinding::strength).reversed().thenComparing(PatternFinding::type));
        return all.size() > limit ? List.copyOf(all.subList(0, limit)) : List.copyOf(all);
    }

    // ---- stress / mood by day of week ------------------------------------------------------------

    public static PatternFinding weekday(List<DailyMetric> days) {
        PatternFinding best = null;
        double bestDiff = 0;
        for (String metric : List.of("STRESS", "MOOD")) {
            Function<DailyMetric, BigDecimal> value = "STRESS".equals(metric) ? DailyMetric::stressScore : DailyMetric::moodScore;
            List<DailyMetric> withValue = days.stream().filter(d -> value.apply(d) != null).toList();
            if (withValue.size() < 14) continue;
            double overall = mean(withValue, value);
            Map<DayOfWeek, List<DailyMetric>> byDay = new EnumMap<>(DayOfWeek.class);
            for (DailyMetric day : withValue) byDay.computeIfAbsent(day.date().getDayOfWeek(), k -> new ArrayList<>()).add(day);
            for (var entry : byDay.entrySet()) {
                if (entry.getValue().size() < 3) continue;
                double dayAverage = mean(entry.getValue(), value);
                double diff = dayAverage - overall;
                if (Math.abs(diff) < WEEKDAY_MIN_DIFF || Math.abs(diff) <= bestDiff) continue;
                bestDiff = Math.abs(diff);
                Map<String, Object> facts = new LinkedHashMap<>();
                facts.put("metric", metric);
                facts.put("weekday", entry.getKey().name());
                facts.put("dayAverage", one(dayAverage));
                facts.put("overallAverage", one(overall));
                facts.put("daySamples", entry.getValue().size());
                List<PatternFinding.EvidencePoint> evidence = entry.getValue().stream()
                        .map(d -> point(d, metric + "_SCORE", value.apply(d))).toList();
                best = new PatternFinding(WEEKDAY, diff > 0 ? "UP" : "DOWN", strength(Math.abs(diff) / 3),
                        withValue.size(), facts, evidence);
            }
        }
        return best;
    }

    // ---- first half vs second half of the window -------------------------------------------------

    public static PatternFinding trendShift(List<DailyMetric> days, LocalDate from, LocalDate to) {
        long span = to.toEpochDay() - from.toEpochDay() + 1;
        LocalDate middle = from.plusDays(span / 2);
        List<DailyMetric> first = days.stream().filter(d -> d.moodScore() != null && d.date().isBefore(middle)).toList();
        List<DailyMetric> second = days.stream().filter(d -> d.moodScore() != null && !d.date().isBefore(middle)).toList();
        if (first.size() < 5 || second.size() < 5) return null;
        double before = mean(first, DailyMetric::moodScore);
        double after = mean(second, DailyMetric::moodScore);
        double diff = after - before;
        if (Math.abs(diff) < TREND_MIN_DIFF) return null;
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("metric", "MOOD");
        facts.put("firstAverage", one(before));
        facts.put("secondAverage", one(after));
        facts.put("firstSamples", first.size());
        facts.put("secondSamples", second.size());
        List<PatternFinding.EvidencePoint> evidence = new ArrayList<>();
        for (DailyMetric d : first) evidence.add(point(d, "MOOD_SCORE", d.moodScore()));
        for (DailyMetric d : second) evidence.add(point(d, "MOOD_SCORE", d.moodScore()));
        return new PatternFinding(TREND, diff > 0 ? "UP" : "DOWN", strength(Math.abs(diff) / 3),
                first.size() + second.size(), facts, evidence);
    }

    // ---- which emotion shows up more often with a topic ------------------------------------------

    public static PatternFinding emotionTopic(List<EntrySignal> entries) {
        List<EntrySignal> analyzed = entries.stream().filter(e -> !e.emotions().isEmpty()).toList();
        int total = analyzed.size();
        if (total < 10) return null;
        Map<String, Integer> emotionCount = new HashMap<>();
        Map<String, Integer> topicCount = new HashMap<>();
        Map<String, Map<String, Integer>> pairs = new TreeMap<>();
        for (EntrySignal e : analyzed) {
            Set<String> emotions = new TreeSet<>(e.emotions());
            Set<String> topics = new TreeSet<>(e.topics());
            emotions.forEach(code -> emotionCount.merge(code, 1, Integer::sum));
            topics.forEach(code -> topicCount.merge(code, 1, Integer::sum));
            for (String topic : topics) for (String emotion : emotions)
                pairs.computeIfAbsent(topic, k -> new TreeMap<>()).merge(emotion, 1, Integer::sum);
        }
        String bestTopic = null, bestEmotion = null;
        double bestLift = 0;
        for (var topicEntry : pairs.entrySet()) {
            String topic = topicEntry.getKey();
            int withTopic = topicCount.get(topic);
            if ("OTHER".equals(topic) || withTopic < 5 || withTopic > total * 0.9) continue;
            for (var emotionEntry : topicEntry.getValue().entrySet()) {
                int together = emotionEntry.getValue();
                if (together < 5) continue;
                double lift = ((double) together / withTopic) / ((double) emotionCount.get(emotionEntry.getKey()) / total);
                if (lift >= MIN_LIFT && lift > bestLift) {
                    bestLift = lift; bestTopic = topic; bestEmotion = emotionEntry.getKey();
                }
            }
        }
        if (bestTopic == null) return null;
        int withTopic = topicCount.get(bestTopic);
        int together = pairs.get(bestTopic).get(bestEmotion);
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("emotion", bestEmotion);
        facts.put("topic", bestTopic);
        facts.put("topicEntries", withTopic);
        facts.put("together", together);
        facts.put("percentWithTopic", Math.round(100.0 * together / withTopic));
        facts.put("percentOverall", Math.round(100.0 * emotionCount.get(bestEmotion) / total));
        facts.put("lift", one(bestLift));
        String topic = bestTopic, emotion = bestEmotion;
        List<PatternFinding.EvidencePoint> evidence = analyzed.stream().filter(e -> e.topics().contains(topic))
                .map(e -> new PatternFinding.EvidencePoint(e.date(), "JOURNAL", e.entryId(), "EMOTION_" + emotion,
                        e.emotions().contains(emotion) ? BigDecimal.ONE : BigDecimal.ZERO)).toList();
        return new PatternFinding(EMOTION_TOPIC, "UP", strength((bestLift - 1) / 1.5), total, facts, evidence);
    }

    // ---- mood of entries with a tag vs without ---------------------------------------------------

    public static PatternFinding tagMood(List<EntrySignal> entries) {
        List<EntrySignal> scored = entries.stream().filter(e -> e.moodScore() != null).toList();
        Set<UUID> tags = new TreeSet<>();
        scored.forEach(e -> tags.addAll(e.tagIds()));
        PatternFinding best = null;
        double bestDiff = 0;
        for (UUID tag : tags) {
            List<EntrySignal> with = scored.stream().filter(e -> e.tagIds().contains(tag)).toList();
            List<EntrySignal> without = scored.stream().filter(e -> !e.tagIds().contains(tag)).toList();
            if (with.size() < 5 || without.size() < 5) continue;
            double withAverage = with.stream().mapToDouble(e -> e.moodScore().doubleValue()).average().orElseThrow();
            double withoutAverage = without.stream().mapToDouble(e -> e.moodScore().doubleValue()).average().orElseThrow();
            double diff = withAverage - withoutAverage;
            if (Math.abs(diff) < GROUP_MIN_DIFF || Math.abs(diff) <= bestDiff) continue;
            bestDiff = Math.abs(diff);
            Map<String, Object> facts = new LinkedHashMap<>();
            facts.put("tagId", tag.toString());
            facts.put("withAverage", one(withAverage));
            facts.put("withoutAverage", one(withoutAverage));
            facts.put("withCount", with.size());
            facts.put("withoutCount", without.size());
            List<PatternFinding.EvidencePoint> evidence = with.stream()
                    .map(e -> new PatternFinding.EvidencePoint(e.date(), "JOURNAL", e.entryId(), "MOOD_SCORE", e.moodScore()))
                    .toList();
            best = new PatternFinding(TAG_MOOD, diff > 0 ? "UP" : "DOWN", strength(Math.abs(diff) / 3),
                    scored.size(), facts, evidence);
        }
        return best;
    }

    // ---- mood on days with a check-in activity vs without ----------------------------------------

    public static PatternFinding activityMood(List<DailyMetric> days, List<ActivitySignal> activities) {
        List<DailyMetric> scored = days.stream().filter(d -> d.moodScore() != null).toList();
        Map<String, Set<LocalDate>> byCode = new TreeMap<>();
        for (ActivitySignal a : activities) byCode.computeIfAbsent(a.code(), k -> new HashSet<>()).add(a.date());
        PatternFinding best = null;
        double bestDiff = 0;
        for (var entry : byCode.entrySet()) {
            List<DailyMetric> with = scored.stream().filter(d -> entry.getValue().contains(d.date())).toList();
            List<DailyMetric> without = scored.stream().filter(d -> !entry.getValue().contains(d.date())).toList();
            if (with.size() < 4 || without.size() < 4) continue;
            double withAverage = mean(with, DailyMetric::moodScore);
            double withoutAverage = mean(without, DailyMetric::moodScore);
            double diff = withAverage - withoutAverage;
            if (Math.abs(diff) < GROUP_MIN_DIFF || Math.abs(diff) <= bestDiff) continue;
            bestDiff = Math.abs(diff);
            Map<String, Object> facts = new LinkedHashMap<>();
            facts.put("activity", entry.getKey());
            facts.put("withAverage", one(withAverage));
            facts.put("withoutAverage", one(withoutAverage));
            facts.put("withDays", with.size());
            facts.put("withoutDays", without.size());
            List<PatternFinding.EvidencePoint> evidence = with.stream().map(d -> point(d, "MOOD_SCORE", d.moodScore())).toList();
            best = new PatternFinding(ACTIVITY_MOOD, diff > 0 ? "UP" : "DOWN", strength(Math.abs(diff) / 3),
                    scored.size(), facts, evidence);
        }
        return best;
    }

    // ---- sleep vs mood (reuses the Pearson association) ------------------------------------------

    public static PatternFinding sleepMood(List<DailyMetric> days, int minimumSamples) {
        var association = MoodSleepAssociation.calculate(days, minimumSamples);
        if (association == null || association.strength().doubleValue() < 0.3) return null;
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("correlation", association.correlation().setScale(2, RoundingMode.HALF_UP));
        List<DailyMetric> shortNights = association.evidence().stream().filter(d -> d.sleepMinutes() < SHORT_SLEEP_MINUTES).toList();
        List<DailyMetric> longerNights = association.evidence().stream().filter(d -> d.sleepMinutes() >= SHORT_SLEEP_MINUTES).toList();
        if (shortNights.size() >= 3 && longerNights.size() >= 3) {
            facts.put("shortSleepMood", one(mean(shortNights, DailyMetric::moodScore)));
            facts.put("longerSleepMood", one(mean(longerNights, DailyMetric::moodScore)));
            facts.put("shortSleepDays", shortNights.size());
        }
        List<PatternFinding.EvidencePoint> evidence = new ArrayList<>();
        for (DailyMetric d : association.evidence()) {
            evidence.add(point(d, "MOOD_SCORE", d.moodScore()));
            evidence.add(point(d, "SLEEP_MINUTES", BigDecimal.valueOf(d.sleepMinutes())));
        }
        return new PatternFinding(SLEEP_MOOD, association.direction(), association.strength(),
                association.sampleSize(), facts, evidence);
    }

    // ---- helpers ---------------------------------------------------------------------------------

    private static double mean(List<DailyMetric> days, Function<DailyMetric, BigDecimal> value) {
        return days.stream().mapToDouble(d -> value.apply(d).doubleValue()).average().orElseThrow();
    }

    private static PatternFinding.EvidencePoint point(DailyMetric day, String metric, BigDecimal value) {
        return new PatternFinding.EvidencePoint(day.date(), "CHECKIN".equals(day.source()) ? "CHECKIN" : "JOURNAL",
                day.sourceId(), metric, value);
    }

    private static BigDecimal one(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
    }

    private static BigDecimal strength(double value) {
        return BigDecimal.valueOf(Math.max(0, Math.min(1, value))).setScale(5, RoundingMode.HALF_UP);
    }
}
