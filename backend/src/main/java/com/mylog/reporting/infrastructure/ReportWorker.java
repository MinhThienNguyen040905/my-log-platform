package com.mylog.reporting.infrastructure;

import com.mylog.reporting.application.ReportService;
import com.mylog.reporting.application.ReportStore;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "mylog.reports", name = "enabled", havingValue = "true")
@ConditionalOnExpression("'${mylog.app-profile:all}' != 'api'")
public class ReportWorker {
    private final ReportStore store;
    private final ReportService reports;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final MeterRegistry metrics;
    private final String workerId = UUID.randomUUID().toString();

    ReportWorker(ReportStore store, ReportService reports, TransactionTemplate tx,
                 Clock clock, MeterRegistry metrics) {
        this.store = store; this.reports = reports; this.tx = tx; this.clock = clock; this.metrics = metrics;
    }

    @Scheduled(fixedDelayString = "${mylog.reports.poll-delay-ms:5000}")
    public void poll() {
        for (int i = 0; i < 10; i++) {
            ReportStore.Task task = tx.execute(s -> store.claim(workerId, clock.instant()));
            if (task == null) return;
            try {
                ReportStore.Result result = reports.calculate(task);
                tx.executeWithoutResult(s -> store.complete(task.id(), workerId, result, clock.instant()));
                metrics.counter("mylog.reports.completed").increment();
            } catch (RuntimeException ignored) {
                tx.executeWithoutResult(s -> store.fail(task.id(), workerId, clock.instant()));
                metrics.counter("mylog.reports.errors").increment();
            }
        }
    }
}
