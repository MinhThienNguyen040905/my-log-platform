package com.mylog.insight.application;

import com.mylog.insight.application.query.MascotMoodView;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/** The Insight-page mascot mirrors the mood the user picked most in the last 7 local days. */
@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class MascotMoodService {
    public static final int WINDOW_DAYS = 7;

    private final DailyAggregateSource source;
    private final Clock clock;

    public MascotMoodService(DailyAggregateSource source, Clock clock) {
        this.source = source; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MascotMoodView current(UUID userId) {
        LocalDate to = LocalDate.now(clock.withZone(ZoneId.of(source.timezone(userId))));
        LocalDate from = to.minusDays(WINDOW_DAYS - 1L);
        var dominant = source.dominantMood(userId, from, to);
        return new MascotMoodView(dominant.code(), dominant.count(), dominant.entries(), from, to,
                source.hasElevatedRisk(userId, from, to));
    }
}
