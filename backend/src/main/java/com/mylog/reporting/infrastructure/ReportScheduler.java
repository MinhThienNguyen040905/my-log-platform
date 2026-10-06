package com.mylog.reporting.infrastructure;

import com.mylog.reporting.application.ReportService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "mylog.reports", name = "enabled", havingValue = "true")
@ConditionalOnExpression("'${mylog.app-profile:all}' != 'api'")
class ReportScheduler {
    private final ReportService reports;

    ReportScheduler(ReportService reports) { this.reports = reports; }

    @Scheduled(fixedDelayString = "${mylog.reports.schedule-delay-ms:3600000}")
    void schedule() { reports.scheduleDue(); }
}
