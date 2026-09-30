package com.mylog.identity.api;

import com.mylog.identity.application.IdentityService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.mylog.platform.security.CurrentUserProvider;
import com.mylog.platform.web.InvalidRequestException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
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

    public record RegisterRequest(@NotBlank @Email @Size(max = 254) String email,
                                  @NotBlank @Size(min = 12, max = 128) String password,
                                  @NotBlank @Size(max = 64) String timezone, @NotBlank @Size(max = 10) String locale,
                                  @NotBlank @Size(max = 40) String termsVersion,
                                  @NotBlank @Size(max = 40) String privacyVersion,
                                  @AssertTrue boolean acceptTerms, @AssertTrue boolean acceptPrivacy) {}
    public record EmailRequest(@NotBlank @Email @Size(max = 254) String email) {}
    public record TokenRequest(@NotBlank @Size(max = 512) String token) {}
    public record LoginRequest(@NotBlank @Email @Size(max = 254) String email, @NotBlank String password,
                               @Size(max = 120) String deviceName) {}
    public record RefreshRequest(@NotBlank @Size(max = 512) String refreshToken) {}

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        UUID id = identity.register(request.email(), request.password(), request.timezone(), request.locale(),
                request.termsVersion(), request.privacyVersion(), http.getRemoteAddr());
        return ResponseEntity.created(URI.create("/api/v1/me")).body(java.util.Map.of("userId", id, "emailVerificationRequired", true));
    }

    @PostMapping("/email-verifications")
    public ResponseEntity<Void> requestVerification(@Valid @RequestBody EmailRequest request, HttpServletRequest http) {
        identity.requestVerification(request.email(), http.getRemoteAddr());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/email-verifications:confirm")
    public ResponseEntity<Void> confirm(@Valid @RequestBody TokenRequest request) {
        identity.confirmVerification(request.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public IdentityService.AuthResult login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return identity.login(request.email(), request.password(), request.deviceName(), http.getRemoteAddr());
    }

    @PostMapping("/refresh")
    public IdentityService.AuthResult refresh(@Valid @RequestBody RefreshRequest request) {
        return identity.refresh(request.refreshToken());
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
