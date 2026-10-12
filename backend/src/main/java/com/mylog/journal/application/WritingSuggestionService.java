package com.mylog.journal.application;

import com.mylog.journal.application.result.WritingSuggestionResult;
import com.mylog.safety.application.SafetyDecision;
import com.mylog.safety.application.SafetyScreeningUseCase;
import com.mylog.user.application.UserProfileUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Ephemeral draft screening. Never stores the draft. */
@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class WritingSuggestionService {
    private final UserProfileUseCase users;
    private final SafetyScreeningUseCase screening;
    private final WritingSuggestionGenerator generator;
    private final boolean enabled;

    public WritingSuggestionService(UserProfileUseCase users, SafetyScreeningUseCase screening,
                                    WritingSuggestionGenerator generator,
                                    @Value("${mylog.writing-suggestions.enabled:false}") boolean enabled) {
        this.users = users;
        this.screening = screening;
        this.generator = generator;
        this.enabled = enabled;
    }

    public WritingSuggestionResult suggest(UUID userId, String text) {
        if (!enabled) return unavailable();
        if (!users.isConsentGranted(userId, "AI_PROCESSING"))
            return new WritingSuggestionResult("CONSENT_REQUIRED", null, null);
        SafetyDecision decision = screening.screen(text);
        return switch (decision.decision()) {
            case "ALLOW" -> allowed(text);
            case "SAFETY_FLOW" -> new WritingSuggestionResult("SAFETY_FLOW", null, null);
            case "CONSTRAIN" -> new WritingSuggestionResult("BLOCKED", null, null);
            default -> unavailable();
        };
    }

    private WritingSuggestionResult allowed(String text) {
        try {
            WritingSuggestionGenerator.Suggestion suggestion = generator.generate(text);
            return new WritingSuggestionResult("SUGGESTION", suggestion.question(), suggestion.promptVersion());
        } catch (RuntimeException ignored) {
            return unavailable();
        }
    }

    private static WritingSuggestionResult unavailable() {
        return new WritingSuggestionResult("UNAVAILABLE", null, null);
    }
}
