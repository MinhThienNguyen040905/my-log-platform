package com.mylog.identity.api;

import com.mylog.identity.application.DevAccountService;
import com.mylog.identity.api.response.DevAccountResponse;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

@RestController
@Hidden
@RequestMapping("/internal/dev/users")
@Profile("local & !staging & !prod")
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class DevAccountController {
    private final DevAccountService accounts;
    private final byte[] key;

    DevAccountController(DevAccountService accounts, Environment environment) {
        this.accounts = accounts;
        String configured = environment.getProperty("MYLOG_DEV_USERS_KEY", "");
        this.key = configured.getBytes(StandardCharsets.UTF_8);
    }

    @GetMapping
    ResponseEntity<List<DevAccountResponse>> list(
            @RequestHeader(value = "X-Dev-Users-Key", required = false) String supplied,
            @RequestParam(required = false) UUID before,
            @RequestParam(defaultValue = "50") int limit) {
        if (key.length < 16 || supplied == null || !MessageDigest.isEqual(key, supplied.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.notFound().build();
        }
        if (limit < 1 || limit > 100) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(accounts.list(before, limit).stream().map(DevAccountResponse::from).toList());
    }
}
