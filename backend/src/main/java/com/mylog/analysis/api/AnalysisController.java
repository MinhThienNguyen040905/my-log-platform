package com.mylog.analysis.api;

import com.mylog.analysis.api.response.AnalysisResponse;
import com.mylog.analysis.api.response.AnalysisRetryResponse;
import com.mylog.analysis.application.AnalysisQueries;
import com.mylog.analysis.application.AnalysisRetryService;
import com.mylog.platform.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/journal-entries/{entryId}")
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class AnalysisController {
    private final AnalysisQueries queries;
    private final AnalysisRetryService retry;
    private final CurrentUserProvider current;

    AnalysisController(AnalysisQueries queries, AnalysisRetryService retry, CurrentUserProvider current) {
        this.queries = queries; this.retry = retry; this.current = current;
    }

    @GetMapping("/analysis")
    AnalysisResponse get(@PathVariable UUID entryId) {
        return AnalysisResponse.from(queries.get(current.requireCurrent().userId(), entryId));
    }

    @PostMapping("/analysis:retry")
    AnalysisRetryResponse retry(@PathVariable UUID entryId) {
        return new AnalysisRetryResponse(retry.retry(current.requireCurrent().userId(), entryId));
    }
}
