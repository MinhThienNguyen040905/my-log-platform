package com.mylog.journal;

import com.mylog.journal.application.WritingSuggestionService;
import com.mylog.safety.application.RiskLevel;
import com.mylog.safety.application.SafetyDecision;
import com.mylog.safety.application.SafetyScreeningUseCase;
import com.mylog.user.application.UserProfileUseCase;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WritingSuggestionServiceTest {
    private final UUID userId = UUID.randomUUID();
    private final UserProfileUseCase users = mock(UserProfileUseCase.class);

    @Test void disabledOrMissingConsentNeverScreensDraft() {
        SafetyScreeningUseCase screening = mock(SafetyScreeningUseCase.class);
        assertEquals("UNAVAILABLE", new WritingSuggestionService(users, screening, false)
                .suggest(userId, "Synthetic journal draft").status());
        assertEquals("CONSENT_REQUIRED", new WritingSuggestionService(users, screening, true)
                .suggest(userId, "Synthetic journal draft").status());
        verifyNoInteractions(screening);
    }

    @Test void onlyAllowReturnsFixedPromptAndRiskNeverLeaksDraft() {
        when(users.isConsentGranted(userId, "AI_PROCESSING")).thenReturn(true);
        AtomicInteger calls = new AtomicInteger();
        SafetyScreeningUseCase screening = text -> {
            calls.incrementAndGet();
            return decision(text.equals("Synthetic risk") ? "SAFETY_FLOW" : "ALLOW");
        };
        var service = new WritingSuggestionService(users, screening, true);
        var safe = service.suggest(userId, "Synthetic ordinary draft");
        assertEquals("SUGGESTION", safe.status());
        assertNotNull(safe.suggestion());
        assertFalse(safe.suggestion().contains("Synthetic"));
        var risk = service.suggest(userId, "Synthetic risk");
        assertEquals("SAFETY_FLOW", risk.status());
        assertNull(risk.suggestion());
        assertEquals(2, calls.get());
    }

    @Test void moderateAndFailureDoNotReturnPrompt() {
        when(users.isConsentGranted(userId, "AI_PROCESSING")).thenReturn(true);
        assertEquals("BLOCKED", new WritingSuggestionService(users,
                text -> decision("CONSTRAIN"), true).suggest(userId, "Synthetic moderate draft").status());
        assertEquals("UNAVAILABLE", new WritingSuggestionService(users,
                text -> decision("FAIL_SAFE"), true).suggest(userId, "Synthetic unknown draft").status());
    }

    private static SafetyDecision decision(String value) {
        return new SafetyDecision(RiskLevel.UNKNOWN, value, "rule-test", null, null,
                "policy-test", null, null);
    }
}
