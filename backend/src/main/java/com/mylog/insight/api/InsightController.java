package com.mylog.insight.api;

import com.mylog.insight.api.response.InsightPageResponse;
import com.mylog.insight.application.InsightService;
import com.mylog.platform.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/insights")
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class InsightController {
    private final InsightService insights;
    private final CurrentUserProvider current;

    InsightController(InsightService insights, CurrentUserProvider current) {
        this.insights = insights; this.current = current;
    }

    @GetMapping
    InsightPageResponse list(@RequestParam(required = false) LocalDate from,
                             @RequestParam(required = false) LocalDate to,
                             @RequestParam(required = false) String cursor,
                             @RequestParam(defaultValue = "20") int limit) {
        return InsightPageResponse.from(insights.list(current.requireCurrent().userId(), from, to, cursor, limit));
    }
}
