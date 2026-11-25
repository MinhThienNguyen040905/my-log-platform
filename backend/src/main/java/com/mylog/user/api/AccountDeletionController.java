package com.mylog.user.api;

import com.mylog.platform.security.CurrentUserProvider;
import com.mylog.user.api.request.RequestDeletionRequest;
import com.mylog.user.api.response.DeletionResponse;
import com.mylog.user.application.AccountDeletionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

/** UC-23. Absent (so 404) unless {@code mylog.account-deletion.enabled=true}. Cancel by signing in again. */
@RestController
@RequestMapping("/api/v1/account-deletion-requests")
@ConditionalOnProperty(prefix = "mylog", name = {"identity.enabled", "account-deletion.enabled"}, havingValue = "true")
public class AccountDeletionController {
    private final AccountDeletionService deletion;
    private final CurrentUserProvider current;
    public AccountDeletionController(AccountDeletionService deletion,CurrentUserProvider current) {
        this.deletion=deletion;this.current=current;
    }
    @PostMapping @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<DeletionResponse> request(@AuthenticationPrincipal Jwt jwt,
                                                    @Valid @RequestBody RequestDeletionRequest request) {
        return ResponseEntity.accepted().body(DeletionResponse.from(
                deletion.request(current.requireCurrent().userId(),sessionId(jwt),request.password())));
    }
    private static UUID sessionId(Jwt jwt) {
        if (jwt == null || !jwt.hasClaim("sid")) return null;
        try { return UUID.fromString(jwt.getClaimAsString("sid")); }
        catch (RuntimeException ignored) { return null; }
    }
}
