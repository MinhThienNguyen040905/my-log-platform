package com.mylog.identity.api;

import com.mylog.identity.application.IdentityService;
import com.mylog.identity.api.request.ConfirmEmailVerificationRequest;
import com.mylog.identity.api.request.EmailVerificationRequest;
import com.mylog.identity.api.request.LoginRequest;
import com.mylog.identity.api.request.RefreshRequest;
import com.mylog.identity.api.request.RegisterRequest;
import com.mylog.identity.api.response.AuthResponse;
import com.mylog.identity.api.response.RegisterResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.mylog.platform.security.CurrentUserProvider;
import com.mylog.platform.web.InvalidRequestException;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class IdentityController {
    private final IdentityService identity;
    private final CurrentUserProvider currentUser;
    public IdentityController(IdentityService identity, CurrentUserProvider currentUser) {
        this.identity = identity; this.currentUser = currentUser;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        UUID id = identity.register(request.email(), request.password(), request.timezone(), request.locale(),
                request.termsVersion(), request.privacyVersion(), http.getRemoteAddr());
        return ResponseEntity.created(URI.create("/api/v1/me")).body(new RegisterResponse(id, true));
    }

    @PostMapping("/email-verifications")
    public ResponseEntity<Void> requestVerification(@Valid @RequestBody EmailVerificationRequest request, HttpServletRequest http) {
        identity.requestVerification(request.email(), http.getRemoteAddr());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/email-verifications:confirm")
    public ResponseEntity<Void> confirm(@Valid @RequestBody ConfirmEmailVerificationRequest request) {
        identity.confirmVerification(request.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return AuthResponse.from(identity.login(request.email(), request.password(), request.deviceName(), http.getRemoteAddr()));
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return AuthResponse.from(identity.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        UUID sessionId = session(jwt);
        identity.logout(currentUser.requireCurrent().userId(), sessionId);
        return ResponseEntity.noContent().build();
    }

    private static UUID session(Jwt jwt) {
        try { return UUID.fromString(jwt.getClaimAsString("sid")); }
        catch (RuntimeException e) { throw new InvalidRequestException(); }
    }
}
