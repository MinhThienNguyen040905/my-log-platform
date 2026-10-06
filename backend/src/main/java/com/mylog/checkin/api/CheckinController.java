package com.mylog.checkin.api;

import com.mylog.checkin.api.request.PutCheckinRequest;
import com.mylog.checkin.api.response.CheckinResponse;
import com.mylog.checkin.application.CheckinService;
import com.mylog.platform.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/check-ins")
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class CheckinController {
    private final CheckinService checkins;
    private final CurrentUserProvider current;

    public CheckinController(CheckinService checkins, CurrentUserProvider current) {
        this.checkins = checkins; this.current = current;
    }

    @PutMapping("/{localDate}")
    public ResponseEntity<CheckinResponse> put(@PathVariable LocalDate localDate,
                                               @Valid @RequestBody PutCheckinRequest request) {
        var view = checkins.put(user(), localDate, request.command());
        return ResponseEntity.ok().eTag("\"" + view.version() + "\"").body(CheckinResponse.from(view));
    }

    @GetMapping("/{localDate}")
    public CheckinResponse get(@PathVariable LocalDate localDate) {
        return CheckinResponse.from(checkins.get(user(), localDate));
    }

    @GetMapping
    public List<CheckinResponse> list(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        return checkins.list(user(), from, to).stream().map(CheckinResponse::from).toList();
    }

    private UUID user() { return current.requireCurrent().userId(); }
}
