package com.mylog.analysis.api;

import com.mylog.analysis.api.response.DevAnalysisSeedResponse;
import com.mylog.analysis.application.DevAnalysisSeedService;
import com.mylog.platform.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Local-only: synthetic emotion/topic analysis for the current user's last 30 days (option 1a). */
@RestController
@Hidden
@RequestMapping("/api/v1/dev")
@Profile("local & !staging & !prod")
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class DevAnalysisSeedController {
    private final DevAnalysisSeedService seeds;
    private final CurrentUserProvider current;

    DevAnalysisSeedController(DevAnalysisSeedService seeds, CurrentUserProvider current) {
        this.seeds = seeds; this.current = current;
    }

    @PostMapping("/analyses:seed")
    DevAnalysisSeedResponse seed(@RequestParam(defaultValue = "false") boolean reseed) {
        return DevAnalysisSeedResponse.from(seeds.seedRecent(current.requireCurrent().userId(), reseed));
    }
}
