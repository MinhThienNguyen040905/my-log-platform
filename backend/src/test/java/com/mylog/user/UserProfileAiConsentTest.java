package com.mylog.user;

import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.platform.id.IdGenerator;
import com.mylog.user.application.UserProfileService;
import com.mylog.user.application.UserStore;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserProfileAiConsentTest {
    @Test void mvpRequiresCurrentGroqConsentVersion() {
        UUID userId = UUID.randomUUID();
        UserStore store = mock(UserStore.class);
        when(store.find(userId)).thenReturn(Optional.of(
                new UserStore.Profile(userId, null, "Asia/Ho_Chi_Minh", "vi", null, 0)));
        UserProfileService service = new UserProfileService(store, mock(SensitiveDataCipher.class),
                mock(IdGenerator.class), Clock.systemUTC(), new ObjectMapper(), true);

        when(store.latestConsents(userId)).thenReturn(List.of(
                new UserStore.Consent("AI_PROCESSING", "demo-v1", true, Instant.EPOCH)));
        assertFalse(service.isConsentGranted(userId, "AI_PROCESSING"));

        when(store.latestConsents(userId)).thenReturn(List.of(
                new UserStore.Consent("AI_PROCESSING", "groq-v1", true, Instant.EPOCH)));
        assertTrue(service.isConsentGranted(userId, "AI_PROCESSING"));

        when(store.latestConsents(userId)).thenReturn(List.of(
                new UserStore.Consent("AI_PROCESSING", "groq-v1", false, Instant.EPOCH)));
        assertFalse(service.isConsentGranted(userId, "AI_PROCESSING"));
    }
}
