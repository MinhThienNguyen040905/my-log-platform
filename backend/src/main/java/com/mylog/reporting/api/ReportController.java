package com.mylog.reporting.api;

import com.mylog.identity.application.IdentityService;
import com.mylog.platform.security.CurrentUserProvider;
import com.mylog.reporting.api.request.CreateReportRequest;
import com.mylog.reporting.api.response.ReportPageResponse;
import com.mylog.reporting.api.response.ReportResponse;
import com.mylog.reporting.application.ReportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final IdentityService identity;

    ReportController(ReportService reports, CurrentUserProvider current, IdentityService identity) {
        this.reports = reports; this.current = current; this.identity = identity;
    }

    @GetMapping
    ReportPageResponse list(@RequestParam(required = false) String type,
                            @RequestParam(required = false) String cursor,
                            @RequestParam(defaultValue = "20") int limit) {
        return ReportPageResponse.from(reports.list(user(), type, cursor, limit));
    }

    @GetMapping("/{reportId}")
    ReportResponse get(@PathVariable UUID reportId) {
        return ReportResponse.from(reports.get(user(), reportId));
    }

    /** Report for the most recent finished week/month; 202 while the worker builds it. */
    @PostMapping
    ResponseEntity<ReportResponse> create(@Valid @RequestBody CreateReportRequest request) {
        identity.checkWriteQuota(user(), "report-create", 10, 3600);
        var view = reports.requestLatest(user(), request.type());
        return ResponseEntity.status("READY".equals(view.status()) ? HttpStatus.OK : HttpStatus.ACCEPTED)
                .body(ReportResponse.from(view));
    }

    /** New immutable version of an existing report; the previous version stays unchanged. */
    @PostMapping("/{reportId}:regenerate")
    ResponseEntity<ReportResponse> regenerate(@PathVariable UUID reportId) {
        identity.checkWriteQuota(user(), "report-create", 10, 3600);
        UUID id = reports.regenerate(user(), reportId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ReportResponse.from(reports.get(user(), id)));
    }

    private UUID user() { return current.requireCurrent().userId(); }
}
