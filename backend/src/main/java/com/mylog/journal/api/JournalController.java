package com.mylog.journal.api;

import com.mylog.journal.application.CreateJournalCommand;
import com.mylog.journal.application.JournalPage;
import com.mylog.journal.application.JournalService;
import com.mylog.journal.application.UpdateJournalCommand;
import com.mylog.journal.application.UpdateJournalCommand.Value;
import com.mylog.journal.domain.JournalEntry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/journals")
@Tag(name = "Journals")
@SecurityRequirement(name = "bearerAuth")
public class JournalController {

    private final JournalService journalService;

    public JournalController(JournalService journalService) {
        this.journalService = journalService;
    }

    @PostMapping
    @Operation(summary = "Create a journal entry")
    @ApiResponse(responseCode = "201", description = "Journal created or idempotently replayed")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "409", description = "Idempotency key conflict")
    public ResponseEntity<JournalResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Optional key scoped to user, method and route")
                    @RequestHeader(name = "Idempotency-Key", required = false)
                    String idempotencyKey,
            @Valid @RequestBody CreateJournalRequest body) {
        JournalEntry entry = journalService.create(
                userId(jwt),
                new CreateJournalCommand(
                        body.title(),
                        body.contentText(),
                        body.contentJson(),
                        body.contentFormat(),
                        body.moodScore(),
                        body.stressScore(),
                        body.energyScore(),
                        body.occurredAt(),
                        body.timezoneAtEntry(),
                        Boolean.TRUE.equals(body.favorite())),
                idempotencyKey);
        return ResponseEntity.created(URI.create("/api/v1/journals/" + entry.getId()))
                .eTag(etag(entry))
                .body(JournalResponse.from(entry));
    }

    @GetMapping
    @Operation(summary = "List the authenticated user's journal history")
    @ApiResponse(responseCode = "200", description = "Cursor page returned")
    @ApiResponse(responseCode = "400", description = "Cursor, limit or date range is invalid")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    public JournalPageResponse history(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        JournalPage page = journalService.history(userId(jwt), cursor, limit, from, to);
        return new JournalPageResponse(
                page.items().stream().map(JournalResponse::from).toList(),
                page.nextCursor(),
                page.hasMore());
    }

    @GetMapping("/{journalId}")
    @Operation(summary = "Get one owned journal entry")
    @ApiResponse(responseCode = "200", description = "Journal returned")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Journal not found")
    public ResponseEntity<JournalResponse> get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID journalId) {
        JournalEntry entry = journalService.get(userId(jwt), journalId);
        return ResponseEntity.ok()
                .eTag(etag(entry))
                .body(JournalResponse.from(entry));
    }

    @PatchMapping("/{journalId}")
    @Operation(summary = "Update an owned journal using optimistic concurrency")
    @ApiResponse(responseCode = "200", description = "Journal updated")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Journal not found")
    @ApiResponse(responseCode = "409", description = "Journal version conflict")
    public ResponseEntity<JournalResponse> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID journalId,
            @Valid @RequestBody UpdateJournalRequest body) {
        JournalEntry entry = journalService.update(
                userId(jwt), journalId, toCommand(body));
        return ResponseEntity.ok()
                .eTag(etag(entry))
                .body(JournalResponse.from(entry));
    }

    @DeleteMapping("/{journalId}")
    @Operation(summary = "Soft-delete an owned journal entry")
    @ApiResponse(responseCode = "204", description = "Journal deleted")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Journal not found")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID journalId) {
        journalService.delete(userId(jwt), journalId);
        return ResponseEntity.noContent().build();
    }

    private UpdateJournalCommand toCommand(UpdateJournalRequest body) {
        return new UpdateJournalCommand(
                body.getVersion(),
                new Value<>(body.titleSupplied(), body.getTitle()),
                new Value<>(body.contentTextSupplied(), body.getContentText()),
                new Value<>(body.contentJsonSupplied(), body.getContentJson()),
                new Value<>(body.contentFormatSupplied(), body.getContentFormat()),
                new Value<>(body.moodScoreSupplied(), body.getMoodScore()),
                new Value<>(body.stressScoreSupplied(), body.getStressScore()),
                new Value<>(body.energyScoreSupplied(), body.getEnergyScore()),
                new Value<>(body.occurredAtSupplied(), body.getOccurredAt()),
                new Value<>(body.timezoneAtEntrySupplied(), body.getTimezoneAtEntry()),
                new Value<>(body.favoriteSupplied(), body.getFavorite()));
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private String etag(JournalEntry entry) {
        return "\"" + entry.getVersion() + "\"";
    }
}
