package com.mylog.journal.api;

import com.mylog.identity.application.IdentityService;
import com.mylog.journal.api.request.WritingSuggestionRequest;
import com.mylog.journal.api.response.WritingSuggestionResponse;
import com.mylog.journal.application.WritingSuggestionService;
import com.mylog.platform.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/journal-writing-suggestions")
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class WritingSuggestionController {
    private final CurrentUserProvider current;
    private final IdentityService identity;
    private final WritingSuggestionService suggestions;

    public WritingSuggestionController(CurrentUserProvider current, IdentityService identity,
                                       WritingSuggestionService suggestions) {
        this.current = current;
        this.identity = identity;
        this.suggestions = suggestions;
    }

    @PostMapping
    public WritingSuggestionResponse suggest(@Valid @RequestBody WritingSuggestionRequest request) {
        var userId = current.requireCurrent().userId();
        identity.checkWriteQuota(userId, "writing-suggestion", 20, 900);
        return WritingSuggestionResponse.from(suggestions.suggest(userId, request.text()));
    }
}
