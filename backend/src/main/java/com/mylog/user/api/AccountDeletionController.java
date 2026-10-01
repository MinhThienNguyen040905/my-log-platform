package com.mylog.user.api;

import com.mylog.platform.security.CurrentUserProvider;
import com.mylog.user.api.request.CancelDeletionRequest;
import com.mylog.user.api.request.RequestDeletionRequest;
import com.mylog.user.api.response.DeletionResponse;
import com.mylog.user.application.AccountDeletionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/account-deletion-requests")
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class AccountDeletionController {
    private final AccountDeletionService deletion;
    private final CurrentUserProvider current;
    public AccountDeletionController(AccountDeletionService deletion,CurrentUserProvider current) {
        this.deletion=deletion;this.current=current;
    }
    @PostMapping @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<DeletionResponse> request(@Valid @RequestBody RequestDeletionRequest request) {
        return ResponseEntity.accepted().body(DeletionResponse.from(
                deletion.request(current.requireCurrent().userId(),request.password())));
    }
    @PostMapping("/{id}:status")
    public DeletionResponse status(@PathVariable UUID id,@Valid @RequestBody CancelDeletionRequest request,
                                   HttpServletRequest http) {
        return DeletionResponse.from(deletion.status(id,request.email(),request.password(),http.getRemoteAddr()));
    }
    @PostMapping("/{id}:cancel")
    public ResponseEntity<Void> cancel(@PathVariable UUID id,@Valid @RequestBody CancelDeletionRequest request,
                                       HttpServletRequest http) {
        deletion.cancel(id,request.email(),request.password(),http.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
