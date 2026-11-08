package com.mylog.journal.api;

import com.mylog.journal.api.response.TitleSuggestionsResponse;
import com.mylog.journal.domain.TitleSuggestionCatalog;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Static title suggestions for the companion panel, available in every session and state.
 * Kept apart from {@code /api/v1/journal-writing-suggestions}: no consent, safety screening or
 * {@code companion-turn} quota, because nothing about the user's writing is sent or read.
 */
@RestController
@Validated
@RequestMapping("/api/v1/companion/title-suggestions")
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class CompanionTitleSuggestionController {
    @GetMapping
    public TitleSuggestionsResponse list(
            @RequestParam(required = false) @Pattern(regexp = "happy|neutral|angry|sad|very_bad") String mood) {
        return new TitleSuggestionsResponse(TitleSuggestionCatalog.forMood(mood));
    }
}
