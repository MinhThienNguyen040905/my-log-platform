package com.mylog.insight.domain;

import com.mylog.insight.application.query.ActivitySignal;
import com.mylog.insight.application.query.DailyMetric;
import com.mylog.insight.application.query.EntrySignal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Detectors that look past the plain "X goes with lower mood" association: days that went well despite a
 * usually heavy condition (and what those days had in common), and changes against the user's own previous
 * window. Like {@link PatternDetectors}, each returns {@code null} when the sample is too small or the effect
 * too weak, and describes co-occurrence only, never causation. Facts hold codes and numbers only.
 */
public final class ContrastDetectors {
    public static final String EXCEPTION = "EXCEPTION_DAYS";
    public static final String CONDITION_SHIFT = "CONDITION_SHIFT";
    public static final String TOPIC_SHIFT = "TOPIC_SHIFT";
    public static final String RECOVERY_SHIFT = "RECOVERY_SHIFT";

    /** Kinds of day markers, used both as a condition ("days with X") and as a feature of exception days. */
    public static final String ACTIVITY = "ACTIVITY";
    public static final String TOPIC = "TOPIC";
    public static final String TAG = "TAG";
    public static final String SHORT_SLEEP = "SHORT_SLEEP";
    public static final String LONG_SLEEP = "LONG_SLEEP";

    static final double CONDITION_MIN_GAP = 0.8;
    static final double SHIFT_MIN_CHANGE = 0.8;
    static final double FEATURE_MIN_DIFF = 0.4;
    static final double TOPIC_MIN_SHARE_DIFF = 0.2;
    static final double RECOVERY_MIN_DIFF = 1.0;
    static final int LOW_MOOD = 4;
    static final int RECOVERED_MOOD = 6;
    static final int MAX_RECOVERY_DAYS = 7;
    static final int LONG_SLEEP_MINUTES = 420;

    private record Marker(String kind, String code) {}
    private static final Comparator<Marker> ORDER = Comparator.comparing(Marker::kind).thenComparing(Marker::code);

    /** One day with a mood score and what was recorded on it. */
    private record Day(DailyMetric metric, double mood, boolean sleepKnown, Set<Marker> markers) {
        boolean has(Marker marker) { return markers.contains(marker); }
    }

    /** Days of a condition and the comparable other days. */
    private record Split(List<Day> with, List<Day> without, double withAverage, double withoutAverage) {
        double gap() { return withAverage - withoutAverage; }
    }

    private ContrastDetectors() {}

    // ---- days that went well despite a usually heavier condition ---------------------------------

    public static PatternFinding exceptionDays(PatternWindow window) {
        List<Day> days = days(window);
        if (days.size() < 8) return null;
        PatternFinding best = null;
        double bestScore = 0;
        int bestExceptions = 0;
        for (Marker condition : markers(days, Set.of(ACTIVITY, TOPIC, TAG, SHORT_SLEEP))) {
            Split split = split(days, condition, 4);
            if (split == null || split.gap() > -CONDITION_MIN_GAP) continue;
            List<Day> fine = split.with().stream().filter(d -> d.mood() >= split.withoutAverage()).toList();
            List<Day> low = split.with().stream().filter(d -> d.mood() < split.withoutAverage()).toList();
            if (fine.size() < 2 || low.size() < 2) continue;

            Marker feature = null;
            double featureDiff = 0;
            int featureDays = 0;
            for (Marker candidate : markers(fine, Set.of(ACTIVITY, TOPIC, TAG, LONG_SLEEP))) {
                if (candidate.equals(condition) || TAG.equals(condition.kind()) && TAG.equals(candidate.kind())
                        || SHORT_SLEEP.equals(condition.kind()) && LONG_SLEEP.equals(candidate.kind())) continue;
                int inFine = count(fine, candidate);
                double diff = (double) inFine / fine.size() - (double) count(low, candidate) / low.size();
                if (inFine < 2 || diff < FEATURE_MIN_DIFF) continue;
                if (diff > featureDiff || diff == featureDiff && inFine > featureDays) {
                    feature = candidate; featureDiff = diff; featureDays = inFine;
                }
            }
            double score = feature == null ? 0.3 : 0.35 + 0.5 * featureDiff;
            if (score < bestScore || score == bestScore && fine.size() <= bestExceptions) continue;
            bestScore = score;
            bestExceptions = fine.size();

            Map<String, Object> facts = new LinkedHashMap<>();
            putMarker(facts, "condition", condition);
            facts.put("withAverage", PatternDetectors.one(split.withAverage()));
            facts.put("withoutAverage", PatternDetectors.one(split.withoutAverage()));
            facts.put("conditionDays", split.with().size());
            facts.put("exceptionDays", fine.size());
            if (feature != null) {
                putMarker(facts, "feature", feature);
                facts.put("featureDays", featureDays);
            }
            best = new PatternFinding(EXCEPTION, "UP", PatternDetectors.strength(score), days.size(), facts,
                    moodEvidence(split.with()));
        }
        return best;
    }

    // ---- a condition weighs more or less than in the previous window -----------------------------

    public static PatternFinding conditionShift(PatternWindow current, PatternWindow previous) {
        List<Day> now = days(current);
        List<Day> before = days(previous);
        PatternFinding best = null;
        double bestChange = 0;
        for (Marker condition : markers(now, Set.of(ACTIVITY, TOPIC, TAG, SHORT_SLEEP))) {
            Split a = split(now, condition, 3);
            Split b = split(before, condition, 3);
            if (a == null || b == null) continue;
            double change = a.gap() - b.gap();
            boolean eased = b.gap() <= -CONDITION_MIN_GAP && change >= SHIFT_MIN_CHANGE;
            boolean heavier = a.gap() <= -CONDITION_MIN_GAP && change <= -SHIFT_MIN_CHANGE;
            if (!eased && !heavier || Math.abs(change) <= bestChange) continue;
            bestChange = Math.abs(change);
            Map<String, Object> facts = new LinkedHashMap<>();
            putMarker(facts, "condition", condition);
            facts.put("nowWith", PatternDetectors.one(a.withAverage()));
            facts.put("nowWithout", PatternDetectors.one(a.withoutAverage()));
            facts.put("prevWith", PatternDetectors.one(b.withAverage()));
            facts.put("prevWithout", PatternDetectors.one(b.withoutAverage()));
            facts.put("nowDays", a.with().size());
            facts.put("prevDays", b.with().size());
            best = new PatternFinding(CONDITION_SHIFT, eased ? "UP" : "DOWN",
                    PatternDetectors.strength(Math.abs(change) / 3), now.size(), facts, moodEvidence(a.with()));
        }
        return best;
    }

    // ---- how often a topic comes up compared with the previous window ----------------------------

    public static PatternFinding topicShift(PatternWindow current, PatternWindow previous) {
        List<EntrySignal> now = analyzed(current.entries());
        List<EntrySignal> before = analyzed(previous.entries());
        if (now.size() < 8 || before.size() < 8) return null;
        Set<String> topics = new TreeSet<>();
        for (EntrySignal e : now) topics.addAll(e.topics());
        for (EntrySignal e : before) topics.addAll(e.topics());
        topics.remove("OTHER");
        PatternFinding best = null;
        double bestDiff = 0;
        for (String topic : topics) {
            long nowCount = now.stream().filter(e -> e.topics().contains(topic)).count();
            long prevCount = before.stream().filter(e -> e.topics().contains(topic)).count();
            double diff = (double) nowCount / now.size() - (double) prevCount / before.size();
            if (Math.abs(diff) < TOPIC_MIN_SHARE_DIFF || Math.max(nowCount, prevCount) < 4
                    || Math.abs(diff) <= bestDiff) continue;
            bestDiff = Math.abs(diff);
            Map<String, Object> facts = new LinkedHashMap<>();
            facts.put("topic", topic);
            facts.put("nowCount", nowCount);
            facts.put("nowTotal", now.size());
            facts.put("prevCount", prevCount);
            facts.put("prevTotal", before.size());
            List<PatternFinding.EvidencePoint> evidence = now.stream()
                    .map(e -> new PatternFinding.EvidencePoint(e.date(), "JOURNAL", e.entryId(), "TOPIC_" + topic,
                            e.topics().contains(topic) ? BigDecimal.ONE : BigDecimal.ZERO)).toList();
            best = new PatternFinding(TOPIC_SHIFT, diff > 0 ? "UP" : "DOWN",
                    PatternDetectors.strength(Math.abs(diff) * 1.5), now.size(), facts, evidence);
        }
        return best;
    }

    // ---- how quickly mood comes back after a low day, now vs before ------------------------------

    public static PatternFinding recoveryShift(PatternWindow current, PatternWindow previous) {
        TreeMap<LocalDate, DailyMetric> series = new TreeMap<>();
        for (DailyMetric d : previous.days()) if (d.moodScore() != null) series.put(d.date(), d);
        for (DailyMetric d : current.days()) if (d.moodScore() != null) series.put(d.date(), d);
        List<List<DailyMetric>> now = new ArrayList<>();
        List<List<DailyMetric>> before = new ArrayList<>();
        Double previousMood = null;
        for (DailyMetric day : series.values()) {
            double mood = day.moodScore().doubleValue();
            if (mood <= LOW_MOOD && (previousMood == null || previousMood > LOW_MOOD)) {
                List<DailyMetric> path = recovery(series, day);
                if (path != null) {
                    if (!day.date().isBefore(current.from()) && !day.date().isAfter(current.to())) now.add(path);
                    else if (!day.date().isBefore(previous.from()) && !day.date().isAfter(previous.to())) before.add(path);
                }
            }
            previousMood = mood;
        }
        if (now.size() < 2 || before.size() < 2) return null;
        double nowDays = averageLength(now);
        double prevDays = averageLength(before);
        double diff = nowDays - prevDays;
        if (Math.abs(diff) < RECOVERY_MIN_DIFF) return null;
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("nowDays", PatternDetectors.one(nowDays));
        facts.put("prevDays", PatternDetectors.one(prevDays));
        facts.put("nowEpisodes", now.size());
        facts.put("prevEpisodes", before.size());
        Map<LocalDate, DailyMetric> shown = new TreeMap<>();
        for (List<DailyMetric> path : now) for (DailyMetric d : path) shown.put(d.date(), d);
        List<PatternFinding.EvidencePoint> evidence = shown.values().stream()
                .map(d -> PatternDetectors.point(d, "MOOD_SCORE", d.moodScore())).toList();
        int scoredNow = (int) current.days().stream().filter(d -> d.moodScore() != null).count();
        return new PatternFinding(RECOVERY_SHIFT, diff < 0 ? "UP" : "DOWN",
                PatternDetectors.strength(Math.abs(diff) / 4), scoredNow, facts, evidence);
    }

    /** True when the exception finding already tells the story of {@code finding}'s condition. */
    public static boolean coveredBy(PatternFinding exception, PatternFinding finding) {
        if (exception == null || finding == exception) return false;
        var e = exception.facts();
        var f = finding.facts();
        return switch (finding.type()) {
            case PatternDetectors.ACTIVITY_MOOD -> ACTIVITY.equals(e.get("conditionKind"))
                    && String.valueOf(f.get("activity")).equals(e.get("conditionCode"));
            case PatternDetectors.TAG_MOOD -> TAG.equals(e.get("conditionKind"))
                    && String.valueOf(f.get("tagId")).equals(e.get("tagId"));
            default -> false;
        };
    }

    // ---- helpers ---------------------------------------------------------------------------------

    private static List<Day> days(PatternWindow window) {
        Map<LocalDate, Set<Marker>> markers = new HashMap<>();
        for (ActivitySignal a : window.activities())
            markers.computeIfAbsent(a.date(), k -> new HashSet<>()).add(new Marker(ACTIVITY, a.code()));
        for (EntrySignal e : window.entries()) {
            Set<Marker> set = markers.computeIfAbsent(e.date(), k -> new HashSet<>());
            for (String topic : e.topics()) if (!"OTHER".equals(topic)) set.add(new Marker(TOPIC, topic));
            for (UUID tag : e.tagIds()) set.add(new Marker(TAG, tag.toString()));
        }
        List<Day> out = new ArrayList<>();
        for (DailyMetric d : window.days()) {
            if (d.moodScore() == null) continue;
            Set<Marker> set = new HashSet<>(markers.getOrDefault(d.date(), Set.of()));
            if (d.sleepMinutes() != null && d.sleepMinutes() < PatternDetectors.SHORT_SLEEP_MINUTES)
                set.add(new Marker(SHORT_SLEEP, ""));
            if (d.sleepMinutes() != null && d.sleepMinutes() >= LONG_SLEEP_MINUTES) set.add(new Marker(LONG_SLEEP, ""));
            out.add(new Day(d, d.moodScore().doubleValue(), d.sleepMinutes() != null, set));
        }
        return out;
    }

    private static Set<Marker> markers(List<Day> days, Set<String> kinds) {
        Set<Marker> out = new TreeSet<>(ORDER);
        for (Day d : days) for (Marker m : d.markers()) if (kinds.contains(m.kind())) out.add(m);
        return out;
    }

    /** Short-sleep days are compared only with other days whose sleep is known. */
    private static Split split(List<Day> days, Marker condition, int minimum) {
        List<Day> pool = SHORT_SLEEP.equals(condition.kind()) ? days.stream().filter(Day::sleepKnown).toList() : days;
        List<Day> with = pool.stream().filter(d -> d.has(condition)).toList();
        List<Day> without = pool.stream().filter(d -> !d.has(condition)).toList();
        if (with.size() < minimum || without.size() < minimum) return null;
        return new Split(with, without, average(with), average(without));
    }

    private static double average(List<Day> days) {
        return days.stream().mapToDouble(Day::mood).average().orElseThrow();
    }

    private static int count(List<Day> days, Marker marker) {
        return (int) days.stream().filter(d -> d.has(marker)).count();
    }

    /** A tag is stored under {@code tagId} (as in the other detectors) so its name can be looked up later. */
    private static void putMarker(Map<String, Object> facts, String role, Marker marker) {
        facts.put(role + "Kind", marker.kind());
        if (TAG.equals(marker.kind())) facts.put("tagId", marker.code());
        else if (!marker.code().isEmpty()) facts.put(role + "Code", marker.code());
    }

    private static List<PatternFinding.EvidencePoint> moodEvidence(List<Day> days) {
        return days.stream().map(d -> PatternDetectors.point(d.metric(), "MOOD_SCORE", d.metric().moodScore())).toList();
    }

    private static List<EntrySignal> analyzed(List<EntrySignal> entries) {
        return entries.stream().filter(e -> !e.topics().isEmpty() || !e.emotions().isEmpty()).toList();
    }

    /** The low day through the first day back at {@link #RECOVERED_MOOD}, or null if that took too long. */
    private static List<DailyMetric> recovery(TreeMap<LocalDate, DailyMetric> series, DailyMetric low) {
        List<DailyMetric> path = new ArrayList<>(List.of(low));
        for (DailyMetric next : series.tailMap(low.date(), false).values()) {
            if (ChronoUnit.DAYS.between(low.date(), next.date()) > MAX_RECOVERY_DAYS) return null;
            path.add(next);
            if (next.moodScore().doubleValue() >= RECOVERED_MOOD) return path;
        }
        return null;
    }

    private static double averageLength(List<List<DailyMetric>> paths) {
        return paths.stream().mapToLong(p -> ChronoUnit.DAYS.between(p.getFirst().date(), p.getLast().date()))
                .average().orElseThrow();
    }
}
