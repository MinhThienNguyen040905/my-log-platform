package com.mylog.reporting.application;

import com.mylog.insight.application.DailyAggregateSource;
import com.mylog.insight.application.InsightService;
import com.mylog.insight.application.NarrativeRewriter;
import com.mylog.insight.application.query.DailyMetric;
import com.mylog.insight.application.query.EntrySignal;
import com.mylog.insight.application.query.TopCode;
import com.mylog.insight.domain.JournalStreak;
import com.mylog.journal.application.JournalService;
import com.mylog.platform.web.InvalidRequestException;
import com.mylog.platform.web.ResourceNotFoundException;
import com.mylog.reporting.application.query.ReportPage;
import com.mylog.reporting.application.query.ReportView;
import com.mylog.reporting.domain.ReportPeriod;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class ReportService {
    static final String TEMPLATE_VERSION = "report-template-v2";

    private final ReportStore reports;
    private final DailyAggregateSource metrics;
    private final InsightService insights;
    private final NarrativeRewriter rewriter;
    private final JournalService journal;
    private final Clock clock;

    public ReportService(ReportStore reports, DailyAggregateSource metrics, InsightService insights,
                         NarrativeRewriter rewriter, JournalService journal, Clock clock) {
        this.reports = reports; this.metrics = metrics; this.insights = insights;
        this.rewriter = rewriter; this.journal = journal; this.clock = clock;
    }

    public void scheduleDue() {
        UUID cursor = null;
        while (true) {
            List<ReportStore.UserZone> page = reports.usersAfter(cursor, 500);
            if (page.isEmpty()) return;
            for (var user : page) {
                LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(user.timezone())));
                if (today.getDayOfWeek() == DayOfWeek.MONDAY) {
                    ReportPeriod period = ReportPeriod.previousWeek(today);
                    reports.enqueue(user.userId(), "WEEKLY", period.start(), period.end(),
                            user.timezone(), false, clock.instant());
                    insights.generate(user.userId(), period.start(), period.end());
                }
                if (today.getDayOfMonth() == 1) {
                    ReportPeriod period = ReportPeriod.previousMonth(today);
                    reports.enqueue(user.userId(), "MONTHLY", period.start(), period.end(),
                            user.timezone(), false, clock.instant());
                    insights.generate(user.userId(), period.start(), period.end());
                }
            }
            cursor = page.getLast().userId();
            if (page.size() < 500) return;
        }
    }

    /**
     * On-demand report for the most recent finished week/month in the user's timezone. Returns the
     * existing report for that period when there already is one (use {@link #regenerate} for a new version).
     */
    public ReportView requestLatest(UUID userId, String type) {
        if (!"WEEKLY".equals(type) && !"MONTHLY".equals(type)) throw new InvalidRequestException();
        String timezone = metrics.timezone(userId);
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(timezone)));
        ReportPeriod period = "WEEKLY".equals(type) ? ReportPeriod.previousWeek(today) : ReportPeriod.previousMonth(today);
        UUID id = reports.enqueue(userId, type, period.start(), period.end(), timezone, false, clock.instant());
        return get(userId, id);
    }

    /**
     * Builds the immutable snapshot for one report version. Runs in the report worker outside any database
     * transaction, so the optional AI rewording does not hold a connection.
     */
    public ReportStore.Result calculate(ReportStore.Task task) {
        var days = metrics.daily(task.userId(), task.from(), task.to());
        JournalStreak.Result streak = JournalStreak.calculate(days, task.to());
        int moodSamples = (int) days.stream().filter(d -> d.moodScore() != null).count();
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("dayCount", days.size());
        snapshot.put("periodDays", ChronoUnit.DAYS.between(task.from(), task.to()) + 1);
        snapshot.put("journalDays", days.stream().filter(d -> d.journalCount() > 0).count());
        snapshot.put("journalEntryCount", days.stream().mapToInt(DailyMetric::journalCount).sum());
        snapshot.put("moodSampleSize", moodSamples);
        snapshot.putAll(averages(days));
        snapshot.put("longestJournalStreak", streak.longestInRange());

        ReportPeriod previous = previousPeriod(task);
        var previousDays = metrics.daily(task.userId(), previous.start(), previous.end());
        Map<String, Object> previousSnapshot = new LinkedHashMap<>();
        previousSnapshot.put("from", previous.start().toString());
        previousSnapshot.put("to", previous.end().toString());
        previousSnapshot.put("dayCount", previousDays.size());
        previousSnapshot.putAll(averages(previousDays));
        snapshot.put("previous", previousSnapshot);

        snapshot.put("topEmotions", withPrevious(metrics.topEmotions(task.userId(), task.from(), task.to(), 5),
                metrics.topEmotions(task.userId(), previous.start(), previous.end(), 10)));
        snapshot.put("topTopics", withPrevious(metrics.topTopics(task.userId(), task.from(), task.to(), 5),
                metrics.topTopics(task.userId(), previous.start(), previous.end(), 10)));

        // Highlights hold dates, scores and entry IDs only; titles are decrypted when the owner opens the report.
        List<DailyMetric> scored = days.stream().filter(d -> d.moodScore() != null).toList();
        if (scored.size() >= 2) {
            List<EntrySignal> entries = metrics.entrySignals(task.userId(), task.from(), task.to());
            DailyMetric best = scored.stream().max(Comparator.comparing(DailyMetric::moodScore)
                    .thenComparing(DailyMetric::date, Comparator.reverseOrder())).orElseThrow();
            DailyMetric lowest = scored.stream().min(Comparator.comparing(DailyMetric::moodScore)
                    .thenComparing(DailyMetric::date)).orElseThrow();
            if (best.moodScore().compareTo(lowest.moodScore()) != 0) {
                snapshot.put("bestDay", highlight(best, entries, true));
                snapshot.put("lowestDay", highlight(lowest, entries, false));
            }
        }

        String narrative = ReportNarratives.render(task.type(), snapshot);
        String generatedBy = "RULE", model = null, promptVersion = TEMPLATE_VERSION;
        // A harder period keeps the reviewed template wording, like a falling trend in pattern insights.
        if (!ReportNarratives.isHarder(snapshot)) {
            var rewrite = rewriter.rewrite(task.userId(), task.from(), task.to(), List.of(new NarrativeRewriter.Draft(
                    "r1", task.type() + "_REPORT", "STABLE", readableFacts(task.type(), snapshot), narrative, null)))
                    .get("r1");
            if (rewrite != null) {
                narrative = rewrite.text(); generatedBy = "AI_ASSISTED"; model = rewrite.model();
                promptVersion = TEMPLATE_VERSION + "+ai";
            }
        }

        List<ReportStore.Evidence> evidence = new ArrayList<>();
        for (var day : days) {
            if (day.moodScore() != null) evidence.add(new ReportStore.Evidence(day.source(), day.sourceId(), day.date(), "MOOD_SCORE", day.moodScore()));
            if (day.stressScore() != null) evidence.add(new ReportStore.Evidence(day.source(), day.sourceId(), day.date(), "STRESS_SCORE", day.stressScore()));
            if (day.energyScore() != null) evidence.add(new ReportStore.Evidence(day.source(), day.sourceId(), day.date(), "ENERGY_SCORE", day.energyScore()));
            if (day.sleepMinutes() != null) evidence.add(new ReportStore.Evidence(day.source(), day.sourceId(), day.date(), "SLEEP_MINUTES", BigDecimal.valueOf(day.sleepMinutes())));
        }
        return new ReportStore.Result(moodSamples, snapshot, narrative, evidence, generatedBy, model, promptVersion);
    }

    public UUID regenerate(UUID userId, UUID reportId) {
        ReportView existing = reports.get(userId, reportId);
        if (!"READY".equals(existing.status()) && !"FAILED".equals(existing.status())) throw new InvalidRequestException();
        return reports.enqueue(userId, existing.type(), existing.from(), existing.to(),
                existing.timezone(), true, clock.instant());
    }

    public ReportPage list(UUID userId, String type, String cursor, int limit) {
        if (type != null && !type.equals("WEEKLY") && !type.equals("MONTHLY") || limit < 1 || limit > 50)
            throw new InvalidRequestException();
        return reports.list(userId, type, cursor, limit);
    }

    /** Owner-scoped; adds the titles of highlighted entries, decrypted for this owner only. */
    public ReportView get(UUID userId, UUID id) {
        ReportView view = reports.get(userId, id);
        Map<String, String> titles = new LinkedHashMap<>();
        for (String key : List.of("bestDay", "lowestDay")) {
            if (view.metrics() != null && view.metrics().get(key) instanceof Map<?, ?> day && day.get("entryId") != null) {
                String entryId = day.get("entryId").toString();
                try {
                    String title = journal.get(userId, UUID.fromString(entryId)).title();
                    if (title != null && !title.isBlank()) titles.put(entryId, title);
                } catch (ResourceNotFoundException | IllegalArgumentException ignored) {
                    // Entry deleted since the snapshot: show the day without a title.
                }
            }
        }
        return view.withEntryTitles(titles);
    }

    static ReportPeriod previousPeriod(ReportStore.Task task) {
        if ("MONTHLY".equals(task.type())) {
            LocalDate start = task.from().minusMonths(1).withDayOfMonth(1);
            return new ReportPeriod(start, start.withDayOfMonth(start.lengthOfMonth()));
        }
        long length = ChronoUnit.DAYS.between(task.from(), task.to()) + 1;
        return new ReportPeriod(task.from().minusDays(length), task.from().minusDays(1));
    }

    private static Map<String, Object> averages(List<DailyMetric> days) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("averageMood", average(days.stream().map(DailyMetric::moodScore).toList()));
        out.put("averageStress", average(days.stream().map(DailyMetric::stressScore).toList()));
        out.put("averageEnergy", average(days.stream().map(DailyMetric::energyScore).toList()));
        out.put("averageSleepMinutes", average(days.stream().map(d -> d.sleepMinutes() == null
                ? null : BigDecimal.valueOf(d.sleepMinutes())).toList()));
        return out;
    }

    private static List<Map<String, Object>> withPrevious(List<TopCode> current, List<TopCode> previous) {
        Map<String, Long> before = new HashMap<>();
        previous.forEach(c -> before.put(c.code(), c.count()));
        return current.stream().map(c -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", c.code());
            item.put("count", c.count());
            item.put("previousCount", before.getOrDefault(c.code(), 0L));
            return item;
        }).toList();
    }

    private static Map<String, Object> highlight(DailyMetric day, List<EntrySignal> entries, boolean best) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("date", day.date().toString());
        out.put("moodScore", day.moodScore());
        out.put("source", day.source());
        entries.stream().filter(e -> e.date().equals(day.date()))
                .max(Comparator.comparing((EntrySignal e) -> e.moodScore() == null ? BigDecimal.ZERO
                        : best ? e.moodScore() : e.moodScore().negate()))
                .ifPresent(e -> out.put("entryId", e.entryId().toString()));
        return out;
    }

    /** Facts for AI wording: numbers and fixed labels only, no journal text or titles. */
    private static Map<String, Object> readableFacts(String type, Map<String, Object> s) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("period", "MONTHLY".equals(type) ? "tháng" : "tuần");
        for (String key : List.of("journalDays", "periodDays", "longestJournalStreak")) out.put(key, s.get(key));
        out.put("averageMood", ReportNarratives.num(ReportNarratives.one(s.get("averageMood"))));
        out.put("averageStress", ReportNarratives.num(ReportNarratives.one(s.get("averageStress"))));
        if (s.get("previous") instanceof Map<?, ?> p) {
            out.put("previousAverageMood", ReportNarratives.num(ReportNarratives.one(p.get("averageMood"))));
            out.put("previousAverageStress", ReportNarratives.num(ReportNarratives.one(p.get("averageStress"))));
        }
        out.values().removeIf(v -> v == null || "".equals(v));
        return out;
    }

    private static BigDecimal average(List<BigDecimal> values) {
        var present = values.stream().filter(Objects::nonNull).toList();
        if (present.isEmpty()) return null;
        return present.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(present.size()), 2, RoundingMode.HALF_UP);
    }
}
