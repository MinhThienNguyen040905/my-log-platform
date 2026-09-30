package com.mylog.user.api;

import com.mylog.identity.application.IdentityService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.mylog.platform.security.CurrentUserProvider;
import com.mylog.platform.web.InvalidRequestException;
import com.mylog.user.application.UserProfileUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
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

    public record UpdateProfileRequest(@Size(max = 120) String displayName, @Size(max = 120) String penName,
                                       List<@Pattern(regexp = "[A-Z_]+") String> onboardingGoals,
                                       String timezone, String locale) {}
    public record ConsentRequest(@NotBlank @Size(max = 40) String documentVersion, boolean granted) {}

    @GetMapping
    public ResponseEntity<UserProfileUseCase.ProfileView> me() { return etag(profiles.get(user())); }

    @PatchMapping
    public ResponseEntity<UserProfileUseCase.ProfileView> update(@Valid @RequestBody UpdateProfileRequest request,
                                                                  @RequestHeader("If-Match") String ifMatch) {
        return etag(profiles.update(user(), request.displayName(), request.penName(), request.onboardingGoals(), request.timezone(),
                request.locale(), version(ifMatch)));
    }

    @PostMapping("/onboarding:complete")
    public ResponseEntity<UserProfileUseCase.ProfileView> complete(@RequestHeader("If-Match") String ifMatch) {
        return etag(profiles.completeOnboarding(user(), version(ifMatch)));
    }

    @GetMapping("/consents")
    public List<UserProfileUseCase.ConsentView> consents() { return profiles.consents(user()); }

    @PutMapping("/consents/{type}")
    public ResponseEntity<Void> consent(@PathVariable String type, @Valid @RequestBody ConsentRequest request) {
        profiles.decideConsent(user(), type, request.documentVersion(), request.granted());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessions")
    public List<IdentityService.SessionView> sessions() { return identity.sessions(user()); }

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
    private static ResponseEntity<UserProfileUseCase.ProfileView> etag(UserProfileUseCase.ProfileView p) {
        return ResponseEntity.ok().eTag("\"" + p.version() + "\"").body(p);
    }
}
