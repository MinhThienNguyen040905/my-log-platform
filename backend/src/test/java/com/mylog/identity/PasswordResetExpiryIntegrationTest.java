package com.mylog.identity;

import com.mylog.TestcontainersConfiguration;
import com.mylog.identity.application.IdentityService;
import com.mylog.identity.application.IdentityStore;
import com.mylog.identity.application.VerificationDelivery;
import com.mylog.platform.crypto.SensitiveDataCipher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * W1-13 (backend/docs/WEB_MOBILE_GAP_TEST_PLAN.md): an expired reset link or code is rejected by the real
 * PostgreSQL query, without waiting an hour. Tokens are written straight through {@link IdentityStore}
 * with expiry instants in the past; the reset itself goes through {@link IdentityService}. Synthetic data only.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {"mylog.identity.enabled=true", "spring.mail.host=localhost", "MYLOG_MAIL_FROM=noreply@mylog.local",
        "logging.level.root=WARN", "logging.level.com.mylog=WARN"})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class PasswordResetExpiryIntegrationTest {
    private static final String NEW_PASSWORD = "A brand new synthetic passphrase";

    @Autowired MockMvc mvc;
    @Autowired IdentityService identity;
    @Autowired IdentityStore store;
    @Autowired SensitiveDataCipher cipher;
    @Autowired TransactionTemplate transactions;
    @MockitoBean VerificationDelivery delivery;

    @Test
    void expiredLinkAndExpiredCodeAreRejected() throws Exception {
        String email = verifiedUser();
        UUID userId = userId(email);
        Instant now = Instant.now();
        String token = "expired-link-" + UUID.randomUUID();
        String code = "13572468";
        transactions.executeWithoutResult(status -> store.createActionToken(UUID.randomUUID(), userId, cipher.tokenHash(token),
                cipher.tokenHash("RESET_PASSWORD_CODE:" + userId + ":" + code), "RESET_PASSWORD",
                now.minus(1, ChronoUnit.SECONDS), now.minus(1, ChronoUnit.SECONDS), now.minus(2, ChronoUnit.HOURS)));

        assertThrows(IdentityService.InvalidCredentialsException.class,
                () -> identity.resetPasswordWithToken(token, NEW_PASSWORD, "203.0.113.10"));
        assertThrows(IdentityService.InvalidCredentialsException.class,
                () -> identity.resetPasswordWithCode(email, code, NEW_PASSWORD, "203.0.113.10"));
    }

    @Test
    void codeExpiresBeforeTheLinkAndTheLinkStillWorks() throws Exception {
        String email = verifiedUser();
        UUID userId = userId(email);
        Instant now = Instant.now();
        String token = "live-link-" + UUID.randomUUID();
        String code = "24681357";
        // Same shape as a real request 20 minutes ago: link valid for 1 hour, code for 15 minutes.
        transactions.executeWithoutResult(status -> store.createActionToken(UUID.randomUUID(), userId, cipher.tokenHash(token),
                cipher.tokenHash("RESET_PASSWORD_CODE:" + userId + ":" + code), "RESET_PASSWORD",
                now.plus(40, ChronoUnit.MINUTES), now.minus(5, ChronoUnit.MINUTES), now.minus(20, ChronoUnit.MINUTES)));

        assertThrows(IdentityService.InvalidCredentialsException.class,
                () -> identity.resetPasswordWithCode(email, code, NEW_PASSWORD, "203.0.113.11"));
        assertDoesNotThrow(() -> identity.resetPasswordWithToken(token, NEW_PASSWORD, "203.0.113.11"));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + NEW_PASSWORD + "\"}"))
                .andExpect(status().isOk());
    }

    private String verifiedUser() throws Exception {
        String email = "reset-expiry-" + UUID.randomUUID() + "@example.test";
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"%s","password":"Correct Horse Battery Staple!","timezone":"UTC",
                         "locale":"en","termsVersion":"v1","privacyVersion":"v1",
                         "acceptTerms":true,"acceptPrivacy":true}
                        """.formatted(email)))
                .andExpect(status().isAccepted());
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(delivery).send(eq(email), anyString(), code.capture());
        mvc.perform(post("/api/v1/auth/email-verifications:confirm-code").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"code\":\"" + code.getValue() + "\"}"))
                .andExpect(status().isNoContent());
        return email;
    }

    private UUID userId(String email) {
        return transactions.execute(status ->
                store.accountByEmailHash(cipher.lookupHash(IdentityService.normalizeEmail(email))).orElseThrow().id());
    }
}
