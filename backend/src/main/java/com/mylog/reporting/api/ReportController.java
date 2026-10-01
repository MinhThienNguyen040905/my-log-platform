package com.mylog.reporting.api;

import com.mylog.platform.security.CurrentUserProvider;
import com.mylog.reporting.api.response.ReportPageResponse;
import com.mylog.reporting.api.response.ReportResponse;
import com.mylog.reporting.application.ReportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class ReportController {
    private final ReportService reports;
    private final CurrentUserProvider current;

    ReportController(ReportService reports, CurrentUserProvider current) {
        this.reports = reports; this.current = current;
    }

    @GetMapping
    ReportPageResponse list(@RequestParam(required = false) String type,
                            @RequestParam(required = false) String cursor,
                            @RequestParam(defaultValue = "20") int limit) {
        return ReportPageResponse.from(reports.list(current.requireCurrent().userId(), type, cursor, limit));
    }

    @GetMapping("/{reportId}")
    ReportResponse get(@PathVariable UUID reportId) {
        return ReportResponse.from(reports.get(current.requireCurrent().userId(), reportId));
    }
}
