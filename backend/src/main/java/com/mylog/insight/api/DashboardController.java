package com.mylog.insight.api;

import com.mylog.insight.api.response.DashboardResponse;
import com.mylog.insight.application.DashboardService;
import com.mylog.platform.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class DashboardController {
    private final DashboardService dashboard;
    private final CurrentUserProvider current;

    DashboardController(DashboardService dashboard, CurrentUserProvider current) {
        this.dashboard = dashboard; this.current = current;
    }

    @GetMapping
    DashboardResponse get(@RequestParam(defaultValue = "30d") String range) {
        return DashboardResponse.from(dashboard.get(current.requireCurrent().userId(), range));
    }
}
