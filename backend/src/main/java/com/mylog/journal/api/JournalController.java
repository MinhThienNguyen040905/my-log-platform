package com.mylog.journal.api;

import com.mylog.journal.api.request.CreateJournalEntryRequest;
import com.mylog.journal.api.request.UpdateJournalEntryRequest;
import com.mylog.journal.api.response.JournalEntryResponse;
import com.mylog.journal.api.response.JournalPageResponse;
import com.mylog.journal.application.JournalService;
import com.mylog.identity.application.IdentityService;
import com.mylog.platform.security.CurrentUserProvider;
import com.mylog.platform.web.InvalidRequestException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/journal-entries")
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class JournalController {
    private final JournalService journal;
    private final CurrentUserProvider current;
    private final IdentityService identity;

    public JournalController(JournalService journal, CurrentUserProvider current, IdentityService identity) {
        this.journal = journal;
        this.current = current;
        this.identity = identity;
    }

    @PostMapping
    public ResponseEntity<JournalEntryResponse> create(@Valid @RequestBody CreateJournalEntryRequest request,
                                                        @RequestHeader("Idempotency-Key") String key) {
        writeQuota();
        var view = journal.create(user(), request.command(), key);
        return ResponseEntity.created(URI.create("/api/v1/journal-entries/" + view.id()))
                .eTag(etag(view.version())).body(JournalEntryResponse.from(view));
    }

    @GetMapping("/{entryId}")
    public ResponseEntity<JournalEntryResponse> get(@PathVariable UUID entryId) {
        var view = journal.get(user(), entryId);
        return ResponseEntity.ok().eTag(etag(view.version())).body(JournalEntryResponse.from(view));
    }

    @GetMapping
    public JournalPageResponse list(@RequestParam(required = false) LocalDate from,
                                    @RequestParam(required = false) LocalDate to,
                                    @RequestParam(required = false) UUID tag,
                                    @RequestParam(required = false) Boolean favorite,
                                    @RequestParam(required = false) String cursor,
                                    @RequestParam(defaultValue = "20") int limit) {
        return JournalPageResponse.from(journal.list(user(), from, to, tag, favorite, cursor, limit));
    }

    @PatchMapping("/{entryId}")
    public ResponseEntity<JournalEntryResponse> update(@PathVariable UUID entryId,
                                                        @RequestHeader("If-Match") String ifMatch,
                                                        @Valid @RequestBody UpdateJournalEntryRequest request) {
        writeQuota();
        var view = journal.update(user(), entryId, request.command(), version(ifMatch));
        return ResponseEntity.ok().eTag(etag(view.version())).body(JournalEntryResponse.from(view));
    }

    @DeleteMapping("/{entryId}")
    public ResponseEntity<Void> delete(@PathVariable UUID entryId, @RequestHeader("If-Match") String ifMatch) {
        writeQuota();
        journal.delete(user(), entryId, version(ifMatch));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{entryId}/favorite")
    public ResponseEntity<JournalEntryResponse> favorite(@PathVariable UUID entryId) {
        writeQuota();
        var view = journal.favorite(user(), entryId, true);
        return ResponseEntity.ok().eTag(etag(view.version())).body(JournalEntryResponse.from(view));
    }

    @DeleteMapping("/{entryId}/favorite")
    public ResponseEntity<JournalEntryResponse> unfavorite(@PathVariable UUID entryId) {
        writeQuota();
        var view = journal.favorite(user(), entryId, false);
        return ResponseEntity.ok().eTag(etag(view.version())).body(JournalEntryResponse.from(view));
    }

    private UUID user() { return current.requireCurrent().userId(); }

    private void writeQuota() { identity.checkWriteQuota(user(), "journal", 60, 900); }

    private static String etag(long version) { return "\"" + version + "\""; }

    private static long version(String ifMatch) {
        if (ifMatch == null || !ifMatch.matches("(?:0|[1-9][0-9]*|\"(?:0|[1-9][0-9]*)\")"))
            throw new InvalidRequestException();
        try { return Long.parseLong(ifMatch.replace("\"", "")); }
        catch (NumberFormatException e) { throw new InvalidRequestException(); }
    }
}
