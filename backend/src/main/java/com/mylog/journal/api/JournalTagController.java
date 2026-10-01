package com.mylog.journal.api;

import com.mylog.journal.api.request.CreateJournalTagRequest;
import com.mylog.journal.api.response.JournalTagResponse;
import com.mylog.journal.application.JournalTagService;
import com.mylog.platform.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class JournalTagController {
    private final JournalTagService tags;
    private final CurrentUserProvider current;

    public JournalTagController(JournalTagService tags, CurrentUserProvider current) {
        this.tags = tags; this.current = current;
    }

    @PostMapping("/api/v1/journal-tags")
    public ResponseEntity<JournalTagResponse> create(@Valid @RequestBody CreateJournalTagRequest request) {
        var tag = tags.create(user(), request.name(), request.color());
        return ResponseEntity.created(URI.create("/api/v1/journal-tags/" + tag.id()))
                .body(JournalTagResponse.from(tag));
    }

    @GetMapping("/api/v1/journal-tags")
    public List<JournalTagResponse> list() {
        return tags.list(user()).stream().map(JournalTagResponse::from).toList();
    }

    @PutMapping("/api/v1/journal-entries/{entryId}/tags/{tagId}")
    public ResponseEntity<Void> attach(@PathVariable UUID entryId, @PathVariable UUID tagId) {
        tags.attach(user(), entryId, tagId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/v1/journal-entries/{entryId}/tags/{tagId}")
    public ResponseEntity<Void> detach(@PathVariable UUID entryId, @PathVariable UUID tagId) {
        tags.detach(user(), entryId, tagId);
        return ResponseEntity.noContent().build();
    }

    private UUID user() { return current.requireCurrent().userId(); }
}
