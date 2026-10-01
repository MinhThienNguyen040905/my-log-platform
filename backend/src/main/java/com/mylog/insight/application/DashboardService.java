package com.mylog.insight.application;

import com.mylog.insight.application.query.DashboardView;
import com.mylog.insight.domain.JournalStreak;
import com.mylog.platform.web.InvalidRequestException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class DashboardService {
    private final DailyAggregateSource source;
    private final Clock clock;

    public DashboardService(DailyAggregateSource source, Clock clock) {
        this.source = source; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardView get(UUID userId, String range) {
        int days = switch (range) {
            case "7d" -> 7;
            case "30d" -> 30;
            case "90d" -> 90;
            default -> throw new InvalidRequestException();
        };
        String timezone = source.timezone(userId);
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(timezone)));
        LocalDate from = today.minusDays(days - 1L);
        var timeline = source.daily(userId, from, today);
        var streak = JournalStreak.calculate(timeline, today);
        return new DashboardView(range, timezone, from, today, timeline,
                source.topEmotions(userId, from, today, 5), source.topTopics(userId, from, today, 5),
                source.currentJournalStreak(userId, today), streak.longestInRange(),
                timeline.stream().mapToInt(d -> d.journalCount()).sum(),
                (int) timeline.stream().filter(d -> d.moodScore() != null).count());
    }
}
