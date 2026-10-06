package com.mylog.feedback.api;

import com.mylog.feedback.api.request.CreateFeedbackRequest;
import com.mylog.feedback.api.request.UpdateFeedbackRequest;
import com.mylog.feedback.api.response.FeedbackCreatedResponse;
import com.mylog.feedback.api.response.FeedbackResponse;
import com.mylog.feedback.application.FeedbackService;
import com.mylog.platform.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import com.mylog.platform.web.InvalidRequestException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@SecurityRequirement(name="bearerAuth")
@ConditionalOnProperty(prefix="mylog.identity",name="enabled",havingValue="true")
public class FeedbackController {
    private final FeedbackService feedback;
    private final CurrentUserProvider current;
    public FeedbackController(FeedbackService feedback,CurrentUserProvider current) {
        this.feedback=feedback;this.current=current;
    }
    @PostMapping("/api/v1/feedback")
    public ResponseEntity<FeedbackCreatedResponse> submit(@Valid @RequestBody CreateFeedbackRequest request) {
        return ResponseEntity.accepted().body(new FeedbackCreatedResponse(
                feedback.submit(user(),request.category(),request.message())));
    }
    @GetMapping("/api/v1/feedback/{id}")
    public FeedbackResponse own(@PathVariable UUID id) {return FeedbackResponse.from(feedback.own(user(),id));}
    @GetMapping("/api/v1/admin/feedback/{id}") @PreAuthorize("hasAuthority('feedback:read')")
    public FeedbackResponse adminGet(@PathVariable UUID id) {return FeedbackResponse.from(feedback.adminGet(user(),id));}
    @GetMapping("/api/v1/admin/feedback") @PreAuthorize("hasAuthority('feedback:read')")
    public List<FeedbackResponse> adminList(@RequestParam(required=false) String status,
                                            @RequestParam(defaultValue="50") int limit) {
        return feedback.adminList(user(),status,limit).stream().map(FeedbackResponse::from).toList();
    }
    @PatchMapping("/api/v1/admin/feedback/{id}") @PreAuthorize("hasAuthority('feedback:manage')")
    public FeedbackResponse adminUpdate(@PathVariable UUID id,@Valid @RequestBody UpdateFeedbackRequest request,
                                        @RequestHeader("If-Match") String ifMatch) {
        long version;
        try {version=Long.parseLong(ifMatch.replace("\"",""));}
        catch (NumberFormatException e) {throw new InvalidRequestException();}
        return FeedbackResponse.from(feedback.adminUpdate(user(),id,request.status(),request.assignedTo(),version));
    }
    private UUID user(){return current.requireCurrent().userId();}
}
