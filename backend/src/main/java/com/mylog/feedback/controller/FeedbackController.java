package com.mylog.feedback.controller;

import com.mylog.feedback.dto.FeedbackRequest;
import com.mylog.feedback.dto.FeedbackResponse;
import com.mylog.feedback.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feedback")
@Tag(name = "Feedback")
@SecurityRequirement(name = "bearerAuth")
public class FeedbackController {

    private final FeedbackService service;

    public FeedbackController(FeedbackService service) {
        this.service = service;
    }

    @PutMapping("/{targetType}/{targetId}")
    @Operation(summary = "Create or replace feedback for a target")
    public FeedbackResponse put(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String targetType,
            @PathVariable UUID targetId,
            @Valid @RequestBody FeedbackRequest request) {
        return FeedbackResponse.from(service.put(
                UUID.fromString(jwt.getSubject()), targetType, targetId, request.value()));
    }
}
