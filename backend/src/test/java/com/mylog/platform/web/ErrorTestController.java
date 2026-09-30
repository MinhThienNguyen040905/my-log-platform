package com.mylog.platform.web;

import com.mylog.platform.security.UnauthenticatedUserException;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Hidden
@RequestMapping("/test/errors")
class ErrorTestController {
    @PostMapping("/validation")
    void validation(@Valid @RequestBody TestRequest ignored) {
    }

    @GetMapping("/not-found")
    void notFound() {
        throw new ResourceNotFoundException("Không tìm thấy bản ghi.");
    }

    @GetMapping("/conflict")
    void conflict() {
        throw new ConflictException("Dữ liệu đã được cập nhật.");
    }

    @GetMapping("/rate-limit")
    void rateLimit() {
        throw new RateLimitExceededException();
    }

    @GetMapping("/dependency")
    void dependency() {
        throw new DependencyUnavailableException();
    }

    @GetMapping("/unauthorized")
    void unauthorized() {
        throw new UnauthenticatedUserException();
    }

    @GetMapping("/forbidden")
    void forbidden() {
        throw new AccessDeniedException("private reason");
    }

    @GetMapping("/unexpected")
    void unexpected() {
        throw new IllegalStateException("database-password-and-sql");
    }

    record TestRequest(@NotBlank String name) {
    }
}
