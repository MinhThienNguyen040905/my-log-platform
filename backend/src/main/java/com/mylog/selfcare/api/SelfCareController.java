package com.mylog.selfcare.api;

import com.mylog.platform.security.CurrentUserProvider;
import com.mylog.platform.web.InvalidRequestException;
import com.mylog.selfcare.api.request.*;
import com.mylog.selfcare.api.response.*;
import com.mylog.selfcare.application.SelfCareService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/self-care")
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class SelfCareController {
    private final SelfCareService service;
    private final CurrentUserProvider current;
    public SelfCareController(SelfCareService service, CurrentUserProvider current) {
        this.service = service; this.current = current;
    }
    @PostMapping("/goals")
    public ResponseEntity<GoalResponse> createGoal(@Valid @RequestBody CreateGoalRequest request) {
        var goal = service.createGoal(user(), request.category(), request.title(), request.description(),
                request.startDate(), request.targetDate());
        return ResponseEntity.created(URI.create("/api/v1/self-care/goals/" + goal.id()))
                .eTag(etag(goal.version())).body(GoalResponse.from(goal));
    }
    @GetMapping("/goals")
    public List<GoalResponse> goals() {
        return service.goals(user()).stream().map(GoalResponse::from).toList();
    }
    @PatchMapping("/goals/{goalId}")
    public ResponseEntity<GoalResponse> updateGoal(@PathVariable UUID goalId, @RequestHeader("If-Match") String ifMatch,
                                                    @Valid @RequestBody UpdateGoalRequest request) {
        var goal = service.updateGoal(user(), goalId, request.title(), request.description(), request.status(),
                request.startDate(), request.targetDate(), version(ifMatch));
        return ResponseEntity.ok().eTag(etag(goal.version())).body(GoalResponse.from(goal));
    }
    @PostMapping("/goals/{goalId}/habits")
    public ResponseEntity<HabitResponse> createHabit(@PathVariable UUID goalId, @Valid @RequestBody CreateHabitRequest request) {
        var habit = service.createHabit(user(), goalId, request.title(), request.targetValue(), request.unit(),
                request.frequencyType(), request.daysOfWeek());
        return ResponseEntity.created(URI.create("/api/v1/self-care/habits/" + habit.id()))
                .eTag(etag(habit.version())).body(HabitResponse.from(habit));
    }
    @PutMapping("/habits/{habitId}/completions/{localDate}")
    public HabitResponse complete(@PathVariable UUID habitId, @PathVariable LocalDate localDate,
                                  @Valid @RequestBody PutCompletionRequest request) {
        return HabitResponse.from(service.putCompletion(user(), habitId, localDate, request.value()));
    }
    @DeleteMapping("/habits/{habitId}/completions/{localDate}")
    public ResponseEntity<Void> undo(@PathVariable UUID habitId, @PathVariable LocalDate localDate) {
        service.deleteCompletion(user(), habitId, localDate);
        return ResponseEntity.noContent().build();
    }
    private UUID user() { return current.requireCurrent().userId(); }
    private static String etag(long version) { return "\"" + version + "\""; }
    private static long version(String value) {
        if (value == null || !value.matches("(?:0|[1-9][0-9]*|\"(?:0|[1-9][0-9]*)\")"))
            throw new InvalidRequestException();
        try { return Long.parseLong(value.replace("\"", "")); }
        catch (NumberFormatException ex) { throw new InvalidRequestException(); }
    }
}
