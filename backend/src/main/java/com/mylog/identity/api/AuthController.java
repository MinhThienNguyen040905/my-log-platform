package com.mylog.identity.api;

import com.mylog.identity.application.AuthRateLimiter;
import com.mylog.identity.application.AuthService;
import com.mylog.identity.application.AuthenticationResult;
import com.mylog.identity.application.ClientMetadata;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Clock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final AuthRateLimiter rateLimiter;
    private final RefreshCookieTransport cookieTransport;
    private final Clock clock;

    public AuthController(
            AuthService authService,
            AuthRateLimiter rateLimiter,
            RefreshCookieTransport cookieTransport,
            Clock clock) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
        this.cookieTransport = cookieTransport;
        this.clock = clock;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a user and start an authenticated session")
    @ApiResponse(responseCode = "201", description = "User registered")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "409", description = "Email already registered")
    @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    @ApiResponse(responseCode = "503", description = "Rate-limit store unavailable")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        rateLimiter.checkRegister(request.getRemoteAddr());
        AuthenticationResult result = authService.register(
                body.email(), body.password(), body.displayName(), metadata(request));
        cookieTransport.write(response, result.tokens().refreshToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse.from(result, clock));
    }

    @PostMapping("/login")
    @Operation(summary = "Log in with email and password")
    @ApiResponse(responseCode = "200", description = "Authenticated")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    @ApiResponse(responseCode = "503", description = "Rate-limit store unavailable")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        rateLimiter.checkLogin(request.getRemoteAddr());
        AuthenticationResult result = authService.login(body.email(), body.password(), metadata(request));
        cookieTransport.write(response, result.tokens().refreshToken());
        return AuthResponse.from(result, clock);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate the refresh token and issue a new access token")
    @ApiResponse(responseCode = "200", description = "Token rotated")
    @ApiResponse(responseCode = "401", description = "Refresh token invalid or reused")
    @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    @ApiResponse(responseCode = "503", description = "Rate-limit store unavailable")
    public AuthResponse refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        rateLimiter.checkRefresh(request.getRemoteAddr());
        AuthenticationResult result = authService.refresh(cookieTransport.read(request), metadata(request));
        cookieTransport.write(response, result.tokens().refreshToken());
        return AuthResponse.from(result, clock);
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the current refresh-token family")
    @ApiResponse(responseCode = "204", description = "Session revoked")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        authService.logout(cookieTransport.read(request));
        cookieTransport.clear(response);
        return ResponseEntity.noContent().build();
    }

    private ClientMetadata metadata(HttpServletRequest request) {
        return new ClientMetadata(request.getRemoteAddr(), request.getHeader("User-Agent"));
    }
}
