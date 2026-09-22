package com.mylog.common.controller;

import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
@Profile({"api", "local"})
public class SystemController {

    private final Clock clock;
    private final String applicationName;

    public SystemController(Clock clock, @Value("${spring.application.name}") String applicationName) {
        this.clock = clock;
        this.applicationName = applicationName;
    }

    @GetMapping("/info")
    SystemInfoResponse info() {
        return new SystemInfoResponse(applicationName, "UP", Instant.now(clock));
    }

    record SystemInfoResponse(String name, String status, Instant timestamp) {}
}
