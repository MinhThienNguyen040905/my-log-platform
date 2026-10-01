package com.mylog.insight.application;

import com.mylog.insight.application.query.InsightPage;
import com.mylog.insight.domain.MoodSleepAssociation;
import com.mylog.platform.web.InvalidRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class InsightService {
    private final DailyAggregateSource source;
    private final InsightStore store;
    private final Clock clock;
    private final int minimumSamples;

    public InsightService(DailyAggregateSource source, InsightStore store, Clock clock,
                          @Value("${mylog.insights.minimum-samples:7}") int minimumSamples) {
        if (minimumSamples < 3 || minimumSamples > 90) throw new IllegalArgumentException("Invalid insight sample threshold");
        this.source = source; this.store = store; this.clock = clock; this.minimumSamples = minimumSamples;
    }

    @Transactional
    public void generate(UUID userId, LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to) || to.isAfter(from.plusDays(89)))
            throw new InvalidRequestException();
        String timezone = source.timezone(userId);
        var association = MoodSleepAssociation.calculate(source.daily(userId, from, to), minimumSamples);
        if (association != null) store.saveAssociation(userId, from, to, timezone, association, clock.instant());
    }

    @Transactional(readOnly = true)
    public InsightPage list(UUID userId, LocalDate from, LocalDate to, String cursor, int limit) {
        if (limit < 1 || limit > 50 || from != null && to != null && from.isAfter(to))
            throw new InvalidRequestException();
        return store.list(userId, from, to, cursor, limit);
    }
}
