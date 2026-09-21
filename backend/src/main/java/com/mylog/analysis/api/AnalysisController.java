package com.mylog.analysis.api;

import com.mylog.analysis.application.AnalysisCommandService;
import com.mylog.analysis.application.AnalysisQueryService;
import com.mylog.analysis.application.CorrectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/journals/{journalId}")
@Tag(name = "Journal analysis")
@SecurityRequirement(name = "bearerAuth")
public class AnalysisController {

    private final AnalysisQueryService query;
    private final AnalysisCommandService commands;
    private final CorrectionService corrections;

    public AnalysisController(
            AnalysisQueryService query, AnalysisCommandService commands, CorrectionService corrections) {
        this.query = query;
        this.commands = commands;
        this.corrections = corrections;
    }

    @GetMapping("/analysis")
    @Operation(summary = "Get current journal analysis and effective corrections")
    public AnalysisResponse analysis(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID journalId) {
        return query.get(userId(jwt), journalId);
    }

    @PostMapping("/analysis/retry")
    @Operation(summary = "Retry a failed current analysis")
    public ResponseEntity<Void> retry(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID journalId) {
        commands.retryAnalysis(userId(jwt), journalId);
        return ResponseEntity.accepted().build();
    }

    @PatchMapping("/corrections")
    @Operation(summary = "Correct effective emotions or topics while preserving AI output")
    public ResponseEntity<Void> correct(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID journalId,
            @Valid @RequestBody CorrectionRequest request) {
        corrections.correct(userId(jwt), journalId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reflections")
    @Operation(summary = "Get the latest reflection batch for the current journal version")
    public ReflectionResponse reflections(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID journalId) {
        return query.reflections(userId(jwt), journalId);
    }

    @PostMapping("/reflections/regenerate")
    @Operation(summary = "Regenerate reflection questions asynchronously")
    public ResponseEntity<Void> regenerate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID journalId) {
        commands.regenerateReflections(userId(jwt), journalId);
        return ResponseEntity.accepted().build();
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
