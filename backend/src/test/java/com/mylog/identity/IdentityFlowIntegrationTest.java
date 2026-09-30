package com.mylog.identity;

import com.mylog.TestcontainersConfiguration;
import com.mylog.identity.application.VerificationDelivery;
import com.mylog.identity.application.IdentityService;
import com.mylog.platform.crypto.SensitiveDataCipher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.junit.jupiter.api.extension.ExtendWith;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {"mylog.identity.enabled=true", "spring.mail.host=localhost", "MYLOG_MAIL_FROM=noreply@mylog.local", "logging.level.root=WARN", "logging.level.com.mylog=WARN"})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class IdentityFlowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired IdentityService identity;
    @Autowired SensitiveDataCipher cipher;
    @MockitoBean VerificationDelivery delivery;

    @Test
    void exportsOpenApiWithIdentityContracts() throws Exception {
        String contract = mvc.perform(get("/internal/openapi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/auth/register']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/me']").exists())
                .andReturn().getResponse().getContentAsString();
        var paths = mapper.readTree(contract).get("paths");
        org.junit.jupiter.api.Assertions.assertFalse(paths.get("/api/v1/auth/register").get("post").has("security"));
        org.junit.jupiter.api.Assertions.assertTrue(paths.get("/api/v1/me").get("get").has("security"));
        Path target = Path.of("target", "openapi");
        Files.createDirectories(target);
        Files.writeString(target.resolve("mylog-v1.json"), contract);
    }

    @Test
    void registrationVerificationLoginRotationAndOwnership() throws Exception {
        String email = "person-" + UUID.randomUUID() + "@example.test";
        String payload = """
                {"email":"%s","password":"Correct Horse Battery Staple!","timezone":"Asia/Ho_Chi_Minh",
                 "locale":"vi","termsVersion":"v1","privacyVersion":"v1",
                 "acceptTerms":true,"acceptPrivacy":true}
                """.formatted(email);
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isConflict());

        org.mockito.ArgumentCaptor<String> token = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(delivery).send(eq(email), token.capture());
        byte[] storedVerification = jdbc.queryForObject("SELECT token_hash FROM auth_action_tokens ORDER BY created_at DESC LIMIT 1", byte[].class);
        org.junit.jupiter.api.Assertions.assertFalse(java.util.Arrays.equals(storedVerification,
                token.getValue().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        mvc.perform(post("/api/v1/auth/email-verifications:confirm").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token.getValue() + "\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/email-verifications:confirm").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token.getValue() + "\"}"))
                .andExpect(status().isNoContent());

        String login = "{\"email\":\"" + email + "\",\"password\":\"Correct Horse Battery Staple!\"}";
        String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String access = mapper.readTree(body).get("accessToken").asText();
        String refresh = mapper.readTree(body).get("refreshToken").asText();
        byte[] storedRefresh = jdbc.queryForObject("SELECT current_token_hash FROM auth_sessions WHERE user_id=?", byte[].class,
                UUID.fromString(mapper.readTree(body).get("userId").asText()));
        org.junit.jupiter.api.Assertions.assertFalse(java.util.Arrays.equals(storedRefresh,
                refresh.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        byte[] storedEmail = jdbc.queryForObject("SELECT encrypted_email FROM users WHERE email_lookup_hash IS NOT NULL AND id=?",
                byte[].class, UUID.fromString(mapper.readTree(body).get("userId").asText()));
        org.junit.jupiter.api.Assertions.assertFalse(java.util.Arrays.equals(storedEmail, email.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        String storedPassword = jdbc.queryForObject("SELECT password_hash FROM users WHERE id=?", String.class,
                UUID.fromString(mapper.readTree(body).get("userId").asText()));
        org.junit.jupiter.api.Assertions.assertNotEquals("Correct Horse Battery Staple!", storedPassword);

        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk()).andExpect(jsonPath("$.locale").value("vi"));
        mvc.perform(patch("/api/v1/me").header("Authorization", "Bearer " + access)
                        .header("If-Match", "\"0\"").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Test User\",\"onboardingGoals\":[\"REFLECTION\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value("Test User"))
                .andExpect(jsonPath("$.onboardingGoals[0]").value("REFLECTION"));
        mvc.perform(patch("/api/v1/me").header("Authorization", "Bearer " + access)
                        .header("If-Match", "\"0\"").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Stale\"}"))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/v1/me/consents/AI_PROCESSING").header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentVersion\":\"v1\",\"granted\":true}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/me/consents").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.type=='AI_PROCESSING')].granted").value(true));

        String otherEmail = "other-" + UUID.randomUUID() + "@example.test";
        String otherRegistration = payload.replace(email, otherEmail);
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(otherRegistration))
                .andExpect(status().isCreated());
        org.mockito.ArgumentCaptor<String> otherVerification = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(delivery).send(eq(otherEmail), otherVerification.capture());
        mvc.perform(post("/api/v1/auth/email-verifications:confirm").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + otherVerification.getValue() + "\"}"))
                .andExpect(status().isNoContent());
        String otherLogin = login.replace(email, otherEmail);
        String otherBody = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(otherLogin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String otherAccess = mapper.readTree(otherBody).get("accessToken").asText();
        String sessions = mvc.perform(get("/api/v1/me/sessions").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String firstSession = mapper.readTree(sessions).get(0).get("id").asText();
        mvc.perform(delete("/api/v1/me/sessions/" + firstSession).header("Authorization", "Bearer " + otherAccess))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + otherAccess))
                .andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value(""));
        String rotated = mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String nextAccess = mapper.readTree(rotated).get("accessToken").asText();
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + nextAccess))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pendingAndSuspendedAccountsCannotGetTokens() throws Exception {
        String email = "blocked-" + UUID.randomUUID() + "@example.test";
        String register = """
                {"email":"%s","password":"Correct Horse Battery Staple!","timezone":"UTC",
                 "locale":"en","termsVersion":"v1","privacyVersion":"v1",
                 "acceptTerms":true,"acceptPrivacy":true}
                """.formatted(email);
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(register))
                .andExpect(status().isCreated());
        String login = "{\"email\":\"" + email + "\",\"password\":\"Correct Horse Battery Staple!\"}";
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
                .andExpect(status().isUnauthorized());
        org.mockito.ArgumentCaptor<String> token = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(delivery).send(eq(email), token.capture());
        mvc.perform(post("/api/v1/auth/email-verifications:confirm").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token.getValue() + "\"}"))
                .andExpect(status().isNoContent());
        String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(mapper.readTree(body).get("userId").asText());
        Integer userRoleCount = jdbc.queryForObject("""
                SELECT count(*) FROM user_roles ur JOIN roles r ON r.id=ur.role_id
                WHERE ur.user_id=? AND r.code='USER'
                """, Integer.class, id);
        org.junit.jupiter.api.Assertions.assertEquals(1, userRoleCount);
        String access = mapper.readTree(body).get("accessToken").asText();
        String refresh = mapper.readTree(body).get("refreshToken").asText();
        jdbc.update("UPDATE users SET status='SUSPENDED' WHERE id=?", id);
        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isUnauthorized());
        jdbc.update("UPDATE users SET status='DELETION_PENDING' WHERE id=?", id);
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void concurrentRegistrationCreatesOneAccount() throws Exception {
        String email = "race-" + UUID.randomUUID() + "@example.test";
        var gate = new java.util.concurrent.CountDownLatch(1);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var attempts = java.util.stream.IntStream.range(0, 2).mapToObj(i -> pool.submit(() -> {
                gate.await();
                try {
                    identity.register(email, "Correct Horse Battery Staple!", "UTC", "en", "v1", "v1", "192.0.2.1");
                    return true;
                } catch (com.mylog.platform.web.ConflictException e) { return false; }
            })).toList();
            gate.countDown();
            int created = 0;
            for (var attempt : attempts) if (attempt.get()) created++;
            org.junit.jupiter.api.Assertions.assertEquals(1, created);
            Integer count = jdbc.queryForObject("SELECT count(*) FROM users WHERE email_lookup_hash=?", Integer.class,
                    cipher.lookupHash(IdentityService.normalizeEmail(email)));
            org.junit.jupiter.api.Assertions.assertEquals(1, count);
        } finally { pool.shutdownNow(); }
    }

    @Test
    void failedLoginsLockAccountEvenWhenRequestReturnsError() throws Exception {
        String email = "locked-" + UUID.randomUUID() + "@example.test";
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","password":"Correct Horse Battery Staple!","timezone":"UTC",
                 "locale":"en","termsVersion":"v1","privacyVersion":"v1",
                 "acceptTerms":true,"acceptPrivacy":true}
                """.formatted(email))).andExpect(status().isCreated());
        org.mockito.ArgumentCaptor<String> token = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(delivery).send(eq(email), token.capture());
        mvc.perform(post("/api/v1/auth/email-verifications:confirm").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token.getValue() + "\"}"))
                .andExpect(status().isNoContent());
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\",\"password\":\"wrong password\"}"))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Correct Horse Battery Staple!\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void authSecretsNeverAppearInApplicationLogs(CapturedOutput output) throws Exception {
        String email = "logs-" + UUID.randomUUID() + "@example.test";
        String password = "Unique Synthetic Password 123!";
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","password":"%s","timezone":"UTC","locale":"en",
                 "termsVersion":"v1","privacyVersion":"v1","acceptTerms":true,"acceptPrivacy":true}
                """.formatted(email, password))).andExpect(status().isCreated());
        org.mockito.ArgumentCaptor<String> token = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(delivery).send(eq(email), token.capture());
        String body = token.getValue();
        org.junit.jupiter.api.Assertions.assertFalse(output.getAll().contains(email));
        org.junit.jupiter.api.Assertions.assertFalse(output.getAll().contains(password));
        org.junit.jupiter.api.Assertions.assertFalse(output.getAll().contains(body));
    }
}
