package com.mylog.journal.application;

import com.mylog.journal.application.result.WritingSuggestionResult;
import com.mylog.safety.application.SafetyDecision;
import com.mylog.safety.application.SafetyScreeningUseCase;
import com.mylog.user.application.UserProfileUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Ephemeral draft screening. Never stores the draft or calls a generative provider. */
@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class WritingSuggestionService {
    private static final String PROMPT_VERSION = "writing-prompts-draft-v1";
    private static final String[] PROMPTS = {
            "Điều gì trong khoảnh khắc đó khiến bạn chú ý nhất?",
            "Bạn muốn kể thêm điều gì về trải nghiệm này?",
            "Khi nhìn lại, điều gì bạn muốn ghi nhớ về hôm nay?"
    };

    private final UserProfileUseCase users;
    private final SafetyScreeningUseCase screening;
    private final boolean enabled;

    public WritingSuggestionService(UserProfileUseCase users, SafetyScreeningUseCase screening,
                                    @Value("${mylog.writing-suggestions.enabled:false}") boolean enabled) {
        this.users = users;
        this.screening = screening;
        this.enabled = enabled;
    }

    public WritingSuggestionResult suggest(UUID userId, String text) {
        if (!enabled) return unavailable();
        if (!users.isConsentGranted(userId, "AI_PROCESSING"))
            return new WritingSuggestionResult("CONSENT_REQUIRED", null, null);
        SafetyDecision decision = screening.screen(text);
        return switch (decision.decision()) {
            case "ALLOW" -> new WritingSuggestionResult("SUGGESTION",
                    PROMPTS[Math.floorMod(text.length() / 40, PROMPTS.length)], PROMPT_VERSION);
            case "SAFETY_FLOW" -> new WritingSuggestionResult("SAFETY_FLOW", null, null);
            case "CONSTRAIN" -> new WritingSuggestionResult("BLOCKED", null, null);
            default -> unavailable();
        };
    }

    private static WritingSuggestionResult unavailable() {
        return new WritingSuggestionResult("UNAVAILABLE", null, null);
    }
}
