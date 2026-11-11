package com.mylog.insight.api;

import com.mylog.identity.application.IdentityService;
import com.mylog.insight.api.request.UpdateInsightRequest;
import com.mylog.insight.api.response.InsightPageResponse;
import com.mylog.insight.application.InsightService;
import com.mylog.platform.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class InsightController {
    private final InsightService insights;
    private final CurrentUserProvider current;
    private final IdentityService identity;

    InsightController(InsightService insights, CurrentUserProvider current, IdentityService identity) {
        this.insights = insights; this.current = current; this.identity = identity;
    }

    @GetMapping("/insights")
    InsightPageResponse list(@RequestParam(required = false) LocalDate from,
                             @RequestParam(required = false) LocalDate to,
                             @RequestParam(required = false) String cursor,
                             @RequestParam(defaultValue = "20") int limit) {
        return InsightPageResponse.from(insights.list(user(), from, to, cursor, limit));
    }

    /** Recomputes the last-30-day pattern insights; rate limited because it may call an AI provider. */
    @PostMapping("/insights:refresh")
    InsightPageResponse refresh() {
        identity.checkWriteQuota(user(), "insight-refresh", 20, 3600);
        return InsightPageResponse.from(insights.refreshRecent(user()));
    }

    @PatchMapping("/insights/{insightId}")
    ResponseEntity<Void> update(@PathVariable UUID insightId, @Valid @RequestBody UpdateInsightRequest request) {
        insights.hide(user(), insightId);
        return ResponseEntity.noContent().build();
    }

    private UUID user() { return current.requireCurrent().userId(); }
}
