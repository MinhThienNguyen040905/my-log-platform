package com.mylog.analysis.api;

import com.mylog.analysis.api.response.DevPatternExtractionResponse;
import com.mylog.analysis.application.PatternExtractionService;
import com.mylog.identity.application.IdentityService;
import com.mylog.platform.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Local-only stand-in for the future PATTERN_EXTRACTION job (see THINKING_PATTERN_RECOGNITION_PLAN.md §10.1):
 * reads the current user's last-30-day entries for thinking patterns. Up to 40 entries per call; call again
 * while {@code more} is true.
 */
@RestController
@Hidden
@RequestMapping("/api/v1/dev")
@Profile("local & !staging & !prod")
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class DevPatternExtractionController {
    private final PatternExtractionService extraction;
    private final CurrentUserProvider current;
    private final IdentityService identity;

    DevPatternExtractionController(PatternExtractionService extraction, CurrentUserProvider current,
                                   IdentityService identity) {
        this.extraction = extraction; this.current = current; this.identity = identity;
    }

    @PostMapping("/pattern-signals:extract")
    DevPatternExtractionResponse extract() {
        var userId = current.requireCurrent().userId();
        // Each call may make up to 40 provider requests.
        identity.checkWriteQuota(userId, "pattern-extraction", 30, 3600);
        return DevPatternExtractionResponse.from(extraction.extractRecent(userId));
    }
}
