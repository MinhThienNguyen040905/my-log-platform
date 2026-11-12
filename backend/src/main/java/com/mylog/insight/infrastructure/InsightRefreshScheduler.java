package com.mylog.insight.infrastructure;

import com.mylog.insight.application.InsightService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Nightly last-30-day pattern refresh in each user's local time. Off by default; never on API-only nodes. */
@Component
@ConditionalOnProperty(prefix = "mylog.insights.refresh", name = "enabled", havingValue = "true")
@ConditionalOnExpression("'${mylog.app-profile:all}' != 'api' and '${mylog.identity.enabled:false}' == 'true'")
class InsightRefreshScheduler {
    private final InsightService insights;
    private final MeterRegistry metrics;
    private final int localHour;
    private final boolean allowAi;

    InsightRefreshScheduler(InsightService insights, MeterRegistry metrics,
                            @Value("${mylog.insights.refresh.local-hour:3}") int localHour,
                            @Value("${mylog.insights.refresh.ai-narrative:false}") boolean allowAi) {
        if (localHour < 0 || localHour > 23) throw new IllegalArgumentException("Invalid insight refresh hour");
        this.insights = insights; this.metrics = metrics; this.localHour = localHour; this.allowAi = allowAi;
    }

    @Scheduled(fixedDelayString = "${mylog.insights.refresh.poll-delay-ms:900000}")
    void refresh() {
        metrics.counter("mylog.insights.scheduled.refreshed").increment(insights.refreshDueUsers(localHour, allowAi));
    }
}
