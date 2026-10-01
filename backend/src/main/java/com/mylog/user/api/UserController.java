package com.mylog.user.api;

import com.mylog.identity.application.IdentityService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.mylog.platform.security.CurrentUserProvider;
import com.mylog.platform.web.InvalidRequestException;
import com.mylog.user.application.UserProfileUseCase;
import com.mylog.user.application.query.ProfileView;
import com.mylog.user.api.request.ConsentDecisionRequest;
import com.mylog.user.api.request.UpdateProfileRequest;
import com.mylog.user.api.response.ConsentResponse;
import com.mylog.user.api.response.ProfileResponse;
import com.mylog.user.api.response.SessionResponse;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me")
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class UserController {
    private final UserProfileUseCase profiles;
    private final IdentityService identity;
    private final CurrentUserProvider current;
    public UserController(UserProfileUseCase profiles, IdentityService identity, CurrentUserProvider current) {
        this.profiles = profiles; this.identity = identity; this.current = current;
    }

    @GetMapping
    public ResponseEntity<ProfileResponse> me() { return etag(profiles.get(user())); }

    @PatchMapping
    public ResponseEntity<ProfileResponse> update(@Valid @RequestBody UpdateProfileRequest request,
                                                                  @RequestHeader("If-Match") String ifMatch) {
        return etag(profiles.update(user(), request.displayName(), request.penName(), request.onboardingGoals(), request.timezone(),
                request.locale(), version(ifMatch)));
    }

    @PostMapping("/onboarding:complete")
    public ResponseEntity<ProfileResponse> complete(@RequestHeader("If-Match") String ifMatch) {
        return etag(profiles.completeOnboarding(user(), version(ifMatch)));
    }

    @GetMapping("/consents")
    public List<ConsentResponse> consents() {
        return profiles.consents(user()).stream().map(ConsentResponse::from).toList();
    }

    @PutMapping("/consents/{type}")
    public ResponseEntity<Void> consent(@PathVariable String type, @Valid @RequestBody ConsentDecisionRequest request) {
        profiles.decideConsent(user(), type, request.documentVersion(), request.granted());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessions")
    public List<SessionResponse> sessions() {
        return identity.sessions(user()).stream().map(SessionResponse::from).toList();
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> revoke(@PathVariable UUID sessionId) {
        identity.revokeSession(user(), sessionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(value = "/sessions", params = "exceptCurrent=true")
    public ResponseEntity<Void> revokeOthers(@AuthenticationPrincipal Jwt jwt) {
        UUID session;
        try { session = UUID.fromString(jwt.getClaimAsString("sid")); }
        catch (RuntimeException e) { throw new InvalidRequestException(); }
        identity.revokeOtherSessions(user(), session);
        return ResponseEntity.noContent().build();
    }

    private UUID user() { return current.requireCurrent().userId(); }
    private static long version(String ifMatch) {
        try { return Long.parseLong(ifMatch.replace("\"", "")); }
        catch (NumberFormatException e) { throw new InvalidRequestException(); }
    }
    private static ResponseEntity<ProfileResponse> etag(ProfileView p) {
        return ResponseEntity.ok().eTag("\"" + p.version() + "\"").body(ProfileResponse.from(p));
    }
}
