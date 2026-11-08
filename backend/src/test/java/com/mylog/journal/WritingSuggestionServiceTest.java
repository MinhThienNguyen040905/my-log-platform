package com.mylog.journal;

import com.mylog.journal.application.CompanionResponder;
import com.mylog.journal.application.WritingSuggestionService;
import com.mylog.journal.application.command.CompanionHistoryEntry;
import com.mylog.journal.application.command.CompanionTurnCommand;
import com.mylog.journal.application.result.CompanionReply;
import com.mylog.journal.domain.CompanionActionCode;
import com.mylog.safety.application.RiskLevel;
import com.mylog.safety.application.SafetyDecision;
import com.mylog.safety.application.SafetyScreeningUseCase;
import com.mylog.user.application.UserProfileUseCase;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WritingSuggestionServiceTest {
    private final UUID userId = UUID.randomUUID();
    private final UserProfileUseCase users = mock(UserProfileUseCase.class);
    private final CompanionResponder responder = mock(CompanionResponder.class);

    @Test void disabledOrMissingConsentNeverScreensDraft() {
        SafetyScreeningUseCase screening = mock(SafetyScreeningUseCase.class);
        assertEquals("UNAVAILABLE", new WritingSuggestionService(users, screening, responder, false)
                .suggest(userId, legacyCommand("Synthetic journal draft")).status());
        assertEquals("CONSENT_REQUIRED", new WritingSuggestionService(users, screening, responder, true)
                .suggest(userId, legacyCommand("Synthetic journal draft")).status());
        verifyNoInteractions(screening, responder);
    }

    @Test void onlyAllowCallsResponderAndNeverLeaksDraft() {
        when(users.isConsentGranted(userId, "AI_PROCESSING")).thenReturn(true);
        AtomicInteger calls = new AtomicInteger();
        SafetyScreeningUseCase screening = text -> {
            calls.incrementAndGet();
            return decision(text.equals("Synthetic risk") ? "SAFETY_FLOW" : "ALLOW");
        };
        CompanionReply fixedReply = new CompanionReply("Mình đang lắng nghe bạn đây.", List.of("Câu hỏi mẫu"),
                null, List.of(CompanionActionCode.TITLE), List.of(), "LISTENING", "TEMPLATE", "companion-template-v1");
        when(responder.respond(any())).thenReturn(fixedReply);
        var service = new WritingSuggestionService(users, screening, responder, true);

        var safe = service.suggest(userId, legacyCommand("Synthetic ordinary draft"));
        assertEquals("REPLY", safe.status());
        assertFalse(safe.reply().reply().contains("Synthetic"));

        var risk = service.suggest(userId, legacyCommand("Synthetic risk"));
        assertEquals("SAFETY_FLOW", risk.status());
        assertNull(risk.reply());
        assertEquals(2, calls.get());
        verify(responder, times(1)).respond(any());
    }

    @Test void moderateAndFailureDoNotCallResponder() {
        when(users.isConsentGranted(userId, "AI_PROCESSING")).thenReturn(true);
        var blocked = new WritingSuggestionService(users, text -> decision("CONSTRAIN"), responder, true)
                .suggest(userId, legacyCommand("Synthetic moderate draft"));
        assertEquals("BLOCKED", blocked.status());
        assertNotNull(blocked.reply());
        assertTrue(blocked.reply().questions().isEmpty());
        assertFalse(blocked.reply().reply().contains("Synthetic"));

        assertEquals("UNAVAILABLE", new WritingSuggestionService(users, text -> decision("FAIL_SAFE"), responder, true)
                .suggest(userId, legacyCommand("Synthetic unknown draft")).status());
        verifyNoInteractions(responder);
    }

    @Test void screeningWindowCoversDraftAndPriorUserTurnsNotJustLatestMessage() {
        when(users.isConsentGranted(userId, "AI_PROCESSING")).thenReturn(true);
        when(responder.respond(any())).thenReturn(new CompanionReply("ok", List.of(), null, List.of(), List.of(),
                "LISTENING", "TEMPLATE", "companion-template-v1"));
        List<String> screened = new java.util.ArrayList<>();
        SafetyScreeningUseCase screening = text -> {
            screened.add(text);
            return decision("ALLOW");
        };
        var service = new WritingSuggestionService(users, screening, responder, true);

        var command = new CompanionTurnCommand("FREE_TEXT", "mới gửi", null, "nội dung bản nháp", "sad",
                null, null, null, null, null,
                List.of(new CompanionHistoryEntry("assistant", "câu trợ lý"),
                        new CompanionHistoryEntry("user", "lượt trước của user")));
        service.suggest(userId, command);

        String window = screened.get(0);
        assertTrue(window.contains("nội dung bản nháp"));
        assertTrue(window.contains("lượt trước của user"));
        assertTrue(window.contains("mới gửi"));
        assertFalse(window.contains("câu trợ lý"));
    }

    private static CompanionTurnCommand legacyCommand(String text) {
        return new CompanionTurnCommand("FREE_TEXT", text, null, null, null, null, null, null, null, null, List.of());
    }

    private static SafetyDecision decision(String value) {
        return new SafetyDecision(RiskLevel.UNKNOWN, value, "rule-test", null, null,
                "policy-test", null, null);
    }
}
