package com.mylog.insight.api;

import com.mylog.identity.application.IdentityService;
import com.mylog.insight.api.request.UpdatePatternRequest;
import com.mylog.insight.api.response.PatternListResponse;
import com.mylog.insight.api.response.PatternPlanResponse;
import com.mylog.insight.application.ThinkingPatternService;
import com.mylog.platform.security.CurrentUserProvider;
import com.mylog.platform.web.InvalidRequestException;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Thinking-pattern cards of the current user (THINKING_PATTERN_RECOGNITION_PLAN.md, API section). */
@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class ThinkingPatternController {
    private final ThinkingPatternService patterns;
    private final CurrentUserProvider current;
    private final IdentityService identity;

    ThinkingPatternController(ThinkingPatternService patterns, CurrentUserProvider current, IdentityService identity) {
        this.patterns = patterns; this.current = current; this.identity = identity;
    }

    @GetMapping("/patterns")
    PatternListResponse list() {
        return PatternListResponse.from(patterns.list(user()));
    }

    @PostMapping("/patterns:refresh")
    PatternListResponse refresh() {
        identity.checkWriteQuota(user(), "pattern-refresh", 20, 3600);
        return PatternListResponse.from(patterns.refresh(user()));
    }

    @PatchMapping("/patterns/{patternId}")
    ResponseEntity<Void> update(@PathVariable UUID patternId, @Valid @RequestBody UpdatePatternRequest request) {
        boolean hide = request.status() != null;
        if (hide == (request.feedback() != null) || hide && request.reason() != null) throw new InvalidRequestException();
        if (hide) patterns.hide(user(), patternId);
        else patterns.feedback(user(), patternId, request.feedback(), request.reason());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/patterns/{patternId}:start-plan")
    PatternPlanResponse startPlan(@PathVariable UUID patternId) {
        return PatternPlanResponse.from(patterns.startPlan(user(), patternId));
    }

    private UUID user() { return current.requireCurrent().userId(); }
}
