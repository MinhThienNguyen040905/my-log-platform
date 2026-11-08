package com.mylog.journal.application;

import com.mylog.journal.application.command.CompanionTurnCommand;
import com.mylog.journal.application.result.CompanionReply;
import com.mylog.journal.application.result.CompanionTurnResult;
import com.mylog.safety.application.SafetyDecision;
import com.mylog.safety.application.SafetyScreeningUseCase;
import com.mylog.user.application.UserProfileUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Ephemeral companion turn: never stores the draft/message/history. Gate order is fixed and must
 * not change: feature flag, consent, safety screening over the whole conversation window, only
 * then the responder. See the companion plan (backend/docs/COMPANION_CHATBOT_PLAN.md).
 */
@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class WritingSuggestionService {
    private static final String NEUTRAL_BLOCKED_REPLY =
            "Mình chưa thể trò chuyện tiếp về điều này ngay bây giờ. Bạn vẫn có thể viết tiếp vào trang nhật ký.";

    private final UserProfileUseCase users;
    private final SafetyScreeningUseCase screening;
    private final CompanionResponder responder;
    private final CompanionHabitSuggestionService habitSuggestions;
    private final boolean enabled;

    @Autowired
    public WritingSuggestionService(UserProfileUseCase users, SafetyScreeningUseCase screening,
                                    CompanionResponder responder,
                                    CompanionHabitSuggestionService habitSuggestions,
                                    @Value("${mylog.writing-suggestions.enabled:false}") boolean enabled) {
        this.users = users;
        this.screening = screening;
        this.responder = responder;
        this.habitSuggestions = habitSuggestions;
        this.enabled = enabled;
    }

    public WritingSuggestionService(UserProfileUseCase users, SafetyScreeningUseCase screening,
                                    CompanionResponder responder,
                                    boolean enabled) {
        this(users, screening, responder, null, enabled);
    }

    public CompanionTurnResult suggest(UUID userId, CompanionTurnCommand command) {
        if (!enabled) return CompanionTurnResult.of("UNAVAILABLE");
        if (!users.isConsentGranted(userId, "AI_PROCESSING")) return CompanionTurnResult.of("CONSENT_REQUIRED");

        SafetyDecision decision = screening.screen(screeningWindow(command));
        return switch (decision.decision()) {
            case "ALLOW" -> {
                CompanionReply reply = responder.respond(command);
                if (habitSuggestions != null) {
                    reply = reply.withHabitSuggestion(habitSuggestions.suggestForTurn(userId, command).orElse(null));
                }
                yield CompanionTurnResult.of("REPLY", reply);
            }
            case "SAFETY_FLOW" -> CompanionTurnResult.of("SAFETY_FLOW");
            case "CONSTRAIN" -> CompanionTurnResult.of("BLOCKED", blockedReply());
            default -> CompanionTurnResult.of("UNAVAILABLE");
        };
    }

    /** Screens the draft plus every user turn already in the conversation, not just the latest message. */
    private static String screeningWindow(CompanionTurnCommand command) {
        StringBuilder window = new StringBuilder();
        if (command.draftText() != null) window.append(command.draftText()).append('\n');
        for (var entry : command.history()) {
            if (entry.isUser()) window.append(entry.text()).append('\n');
        }
        if (command.message() != null) window.append(command.message());
        return window.toString();
    }

    private static CompanionReply blockedReply() {
        return new CompanionReply(NEUTRAL_BLOCKED_REPLY, List.of(), null, List.of(), List.of(),
                "LISTENING", "TEMPLATE", "companion-blocked-v1");
    }
}
