package com.mylog.knowledge.api;

import com.mylog.knowledge.api.response.RecommendationResponse;
import com.mylog.knowledge.application.RecommendationService;
import com.mylog.platform.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recommendations")
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class RecommendationController {
    private final RecommendationService service;
    private final CurrentUserProvider current;
    public RecommendationController(RecommendationService service, CurrentUserProvider current) {
        this.service=service; this.current=current;
    }
    @GetMapping
    public RecommendationResponse get(@RequestParam String topicCode) {
        return RecommendationResponse.from(topicCode,service.passages(current.requireCurrent().userId(),topicCode));
    }
}
