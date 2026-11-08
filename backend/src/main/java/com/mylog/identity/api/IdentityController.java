package com.mylog.identity.api;

import com.mylog.identity.application.IdentityService;
import com.mylog.identity.api.request.ConfirmEmailVerificationRequest;
import com.mylog.identity.api.request.ConfirmEmailVerificationCodeRequest;
import com.mylog.identity.api.request.EmailVerificationRequest;
import com.mylog.identity.api.request.GoogleAuthRequest;
import com.mylog.identity.api.request.LoginRequest;
import com.mylog.identity.api.request.RefreshRequest;
import com.mylog.identity.api.request.RegisterRequest;
import com.mylog.identity.api.request.PasswordResetRequest;
import com.mylog.identity.api.request.ConfirmPasswordResetRequest;
import com.mylog.identity.api.request.ConfirmPasswordResetCodeRequest;
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

import java.util.Map;
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
        return ResponseEntity.accepted().body(new RegisterResponse(id, true));
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

    @PostMapping("/email-verifications:confirm-code")
    public ResponseEntity<Void> confirmCode(@Valid @RequestBody ConfirmEmailVerificationCodeRequest request,
                                             HttpServletRequest http) {
        identity.confirmVerificationCode(request.email(), request.code(), http.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-resets")
    public ResponseEntity<Map<String, String>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request,
            HttpServletRequest http) {
        identity.requestPasswordReset(request.email(), http.getRemoteAddr());
        return ResponseEntity.accepted().body(Map.of(
                "message", "Nếu tài khoản tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi đến email của bạn."
        ));
    }

    @PostMapping("/password-resets:confirm")
    public ResponseEntity<Void> confirmPasswordReset(
            @Valid @RequestBody ConfirmPasswordResetRequest request,
            HttpServletRequest http) {
        identity.resetPasswordWithToken(request.token(), request.newPassword(), http.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-resets:confirm-code")
    public ResponseEntity<Void> confirmPasswordResetCode(
            @Valid @RequestBody ConfirmPasswordResetCodeRequest request,
            HttpServletRequest http) {
        identity.resetPasswordWithCode(request.email(), request.code(), request.newPassword(), http.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/google")
    public AuthResponse loginWithGoogle(@Valid @RequestBody GoogleAuthRequest request, HttpServletRequest http) {
        return AuthResponse.from(identity.loginWithGoogle(request, http.getRemoteAddr()));
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
