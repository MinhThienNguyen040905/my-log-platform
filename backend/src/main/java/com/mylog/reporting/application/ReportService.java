package com.mylog.reporting.application;

import com.mylog.insight.application.DailyAggregateSource;
import com.mylog.insight.application.InsightService;
import com.mylog.insight.domain.JournalStreak;
import com.mylog.platform.web.InvalidRequestException;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class ReportService {
    private final ReportStore reports;
    private final DailyAggregateSource metrics;
    private final InsightService insights;
    private final Clock clock;

    public ReportService(ReportStore reports, DailyAggregateSource metrics, InsightService insights, Clock clock) {
        this.reports = reports; this.metrics = metrics; this.insights = insights; this.clock = clock;
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

    public ReportStore.Result calculate(ReportStore.Task task) {
        var days = metrics.daily(task.userId(), task.from(), task.to());
        LocalDate reference = task.to();
        JournalStreak.Result streak = JournalStreak.calculate(days, reference);
        int moodSamples = (int) days.stream().filter(d -> d.moodScore() != null).count();
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("dayCount", days.size());
        snapshot.put("journalEntryCount", days.stream().mapToInt(d -> d.journalCount()).sum());
        snapshot.put("moodSampleSize", moodSamples);
        snapshot.put("averageMood", average(days.stream().map(d -> d.moodScore()).toList()));
        snapshot.put("averageStress", average(days.stream().map(d -> d.stressScore()).toList()));
        snapshot.put("averageEnergy", average(days.stream().map(d -> d.energyScore()).toList()));
        snapshot.put("averageSleepMinutes", average(days.stream().map(d -> d.sleepMinutes() == null
                ? null : BigDecimal.valueOf(d.sleepMinutes())).toList()));
        snapshot.put("longestJournalStreak", streak.longestInRange());
        snapshot.put("topEmotions", metrics.topEmotions(task.userId(), task.from(), task.to(), 5)
                .stream().map(c -> Map.of("code", c.code(), "count", c.count())).toList());
        snapshot.put("topTopics", metrics.topTopics(task.userId(), task.from(), task.to(), 5)
                .stream().map(c -> Map.of("code", c.code(), "count", c.count())).toList());
        String narrative = "Trong kỳ này, bạn có " + days.size() + " ngày có dữ liệu và "
                + snapshot.get("journalEntryCount") + " bài nhật ký. "
                + (moodSamples == 0 ? "Chưa đủ điểm tâm trạng để tính trung bình."
                : "Điểm tâm trạng trung bình là " + snapshot.get("averageMood") + " trên 10.");
        List<ReportStore.Evidence> evidence = new ArrayList<>();
        for (var day : days) {
            if (day.moodScore() != null) evidence.add(new ReportStore.Evidence(day.source(), day.sourceId(), day.date(), "MOOD_SCORE", day.moodScore()));
            if (day.stressScore() != null) evidence.add(new ReportStore.Evidence(day.source(), day.sourceId(), day.date(), "STRESS_SCORE", day.stressScore()));
            if (day.energyScore() != null) evidence.add(new ReportStore.Evidence(day.source(), day.sourceId(), day.date(), "ENERGY_SCORE", day.energyScore()));
            if (day.sleepMinutes() != null) evidence.add(new ReportStore.Evidence(day.source(), day.sourceId(), day.date(), "SLEEP_MINUTES", BigDecimal.valueOf(day.sleepMinutes())));
        }
        return new ReportStore.Result(moodSamples, snapshot, narrative, evidence);
    }

    public UUID regenerate(UUID userId, UUID reportId) {
        ReportView existing = reports.get(userId, reportId);
        if (!"READY".equals(existing.status())) throw new InvalidRequestException();
        return reports.enqueue(userId, existing.type(), existing.from(), existing.to(),
                existing.timezone(), true, clock.instant());
    }

    public ReportPage list(UUID userId, String type, String cursor, int limit) {
        if (type != null && !type.equals("WEEKLY") && !type.equals("MONTHLY") || limit < 1 || limit > 50)
            throw new InvalidRequestException();
        return reports.list(userId, type, cursor, limit);
    }

    public ReportView get(UUID userId, UUID id) { return reports.get(userId, id); }

    private static BigDecimal average(List<BigDecimal> values) {
        var present = values.stream().filter(java.util.Objects::nonNull).toList();
        if (present.isEmpty()) return null;
        return present.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(present.size()), 2, RoundingMode.HALF_UP);
    }
}
