package com.mylog.insight.controller;

import com.mylog.insight.dto.InsightResponse;
import com.mylog.insight.service.InsightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/insights")
@Tag(name = "Insights")
@SecurityRequirement(name = "bearerAuth")
public class InsightController {

    private final InsightService service;

    public InsightController(InsightService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List active and fading insights")
    public List<InsightResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return service.list(userId(jwt)).stream().map(InsightResponse::from).toList();
    }

    @GetMapping("/{insightId}")
    @Operation(summary = "Get an insight with traceable evidence and suggested action")
    public InsightResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID insightId) {
        return InsightResponse.from(service.get(userId(jwt), insightId));
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
