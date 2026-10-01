package com.mylog.safety.api;

import com.mylog.safety.api.response.SafetyResourceResponse;
import com.mylog.safety.application.SafetyResourceService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/safety/resources")
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class SafetyResourceController {
    private final SafetyResourceService resources;

    public SafetyResourceController(SafetyResourceService resources) { this.resources = resources; }

    @GetMapping
    public List<SafetyResourceResponse> approved(@RequestParam String locale, @RequestParam String country) {
        return resources.approvedFor(locale, country).stream().map(SafetyResourceResponse::from).toList();
    }
}
