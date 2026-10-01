package com.mylog.admin.api;

import com.mylog.admin.api.request.AdminActionRequest;
import com.mylog.admin.api.response.*;
import com.mylog.admin.application.AdminDashboardService;
import com.mylog.analysis.application.AdminJobService;
import com.mylog.identity.application.AdminAccountService;
import com.mylog.platform.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class AdminController {
    private final AdminAccountService accounts;
    private final AdminJobService jobs;
    private final AdminDashboardService dashboard;
    private final CurrentUserProvider current;
    public AdminController(AdminAccountService accounts,AdminJobService jobs,
                           AdminDashboardService dashboard,CurrentUserProvider current) {
        this.accounts=accounts; this.jobs=jobs; this.dashboard=dashboard; this.current=current;
    }
    @GetMapping("/users") @PreAuthorize("hasAuthority('users:read-metadata')")
    public AdminUsersResponse users(@RequestParam(required=false) UUID cursor,@RequestParam(defaultValue="20") int limit) {
        var list=accounts.list(cursor,limit); return new AdminUsersResponse(list.stream().map(AdminUserResponse::from).toList(),
                list.size()==limit ? list.getLast().id() : null);
    }
    @GetMapping("/users/{userId}/metadata") @PreAuthorize("hasAuthority('users:read-metadata')")
    public AdminUserResponse metadata(@PathVariable UUID userId) { return AdminUserResponse.from(accounts.get(userId)); }
    @PostMapping("/users/{userId}:suspend") @PreAuthorize("hasAuthority('users:suspend')")
    public AdminUserResponse suspend(@PathVariable UUID userId,@Valid @RequestBody AdminActionRequest r) {
        return AdminUserResponse.from(accounts.suspend(user(),userId,r.reasonCode()));
    }
    @PostMapping("/users/{userId}:restore") @PreAuthorize("hasAuthority('users:suspend')")
    public AdminUserResponse restore(@PathVariable UUID userId,@Valid @RequestBody AdminActionRequest r) {
        return AdminUserResponse.from(accounts.restore(user(),userId,r.reasonCode()));
    }
    @GetMapping("/ai-jobs") @PreAuthorize("hasAuthority('jobs:read')")
    public AdminJobsResponse jobs(@RequestParam(required=false) UUID cursor,@RequestParam(defaultValue="20") int limit) {
        var list=jobs.list(cursor,limit); return new AdminJobsResponse(list.stream().map(AdminJobResponse::from).toList(),
                list.size()==limit ? list.getLast().id() : null);
    }
    @PostMapping("/ai-jobs/{jobId}:retry") @PreAuthorize("hasAuthority('jobs:retry')")
    public ResponseEntity<Void> retry(@PathVariable UUID jobId) {
        jobs.retry(user(),jobId); return ResponseEntity.accepted().build();
    }
    @GetMapping("/dashboard") @PreAuthorize("hasAuthority('admin:dashboard')")
    public AdminDashboardResponse dashboard() { return AdminDashboardResponse.from(dashboard.get()); }
    private UUID user() { return current.requireCurrent().userId(); }
}
