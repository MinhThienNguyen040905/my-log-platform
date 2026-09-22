package com.mylog.statistics.controller;

import com.mylog.statistics.dto.EmotionStatisticsResponse;
import com.mylog.statistics.dto.MoodStatisticsResponse;
import com.mylog.statistics.dto.TopicStatisticsResponse;
import com.mylog.statistics.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/statistics")
@Tag(name = "Statistics")
@SecurityRequirement(name = "bearerAuth")
public class StatisticsController {

    private final StatisticsService service;

    public StatisticsController(StatisticsService service) {
        this.service = service;
    }

    @GetMapping("/mood")
    @Operation(summary = "Get mood trend and day-of-week pattern")
    public MoodStatisticsResponse mood(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String timezone) {
        return MoodStatisticsResponse.from(service.mood(userId(jwt), from, to, timezone));
    }

    @GetMapping("/emotions")
    @Operation(summary = "Get effective emotion distribution")
    public EmotionStatisticsResponse emotions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String timezone) {
        return EmotionStatisticsResponse.from(service.emotions(userId(jwt), from, to, timezone));
    }

    @GetMapping("/topics")
    @Operation(summary = "Get effective topic frequency")
    public TopicStatisticsResponse topics(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String timezone) {
        return TopicStatisticsResponse.from(service.topics(userId(jwt), from, to, timezone));
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
