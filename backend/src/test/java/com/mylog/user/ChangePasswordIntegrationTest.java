package com.mylog.user;

import com.mylog.TestcontainersConfiguration;
import com.mylog.identity.application.IdentityStore;
import com.mylog.identity.application.VerificationDelivery;
import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.user.api.request.ChangePasswordRequest;
import com.mylog.user.application.UserProfileUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({TestcontainersConfiguration.class, ChangePasswordIntegrationTest.FixedClock.class})
@SpringBootTest(properties = {
        "mylog.identity.enabled=true",
        "mylog.exports.enabled=true",
        "mylog.deletion.enabled=true",
        "spring.mail.host=localhost",
        "MYLOG_MAIL_FROM=noreply@mylog.local",
        "logging.level.root=WARN"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ChangePasswordIntegrationTest {
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    static class FixedClock {
        @Bean @Primary Clock clockChangePassword() {
            return Clock.fixed(Instant.parse("2026-10-10T00:00:00Z"), ZoneId.of("UTC"));
        }
    }

    private static final Instant NOW = Instant.parse("2026-10-10T00:00:00Z");
    private static final String OLD_PASS = "CurrentValidPassword123!";
    private static final String NEW_PASS = "NewSuperSecretPassword456!";

    @Autowired private JdbcTemplate jdbc;
    @Autowired private SensitiveDataCipher cipher;
    @Autowired private UserProfileUseCase profiles;
    @Autowired private IdentityStore identityStore;
    @Autowired private MockMvc mvc;
    @MockitoBean private VerificationDelivery delivery;

    @Test
    @DisplayName("Đổi mật khẩu thành công: 204 No Content, cập nhật password_hash, thu hồi các phiên khác và giữ phiên hiện tại")
    void shouldChangePasswordSuccessfully() throws Exception {
        UUID userId = createUser("user-change-pwd@example.test", OLD_PASS);
        UUID currentSession = createSession(userId, "Web Browser A");
        UUID otherSession = createSession(userId, "Mobile App B");

        String requestBody = "{\"currentPassword\":\"" + OLD_PASS + "\",\"newPassword\":\"" + NEW_PASS + "\"}";

        mvc.perform(post("/api/v1/me/password")
                        .with(jwt().jwt(j -> j.subject(userId.toString()).claim("sid", currentSession.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNoContent());

        String newHash = jdbc.queryForObject("SELECT password_hash FROM users WHERE id=?", String.class, userId);
        assertNotEquals(OLD_PASS, newHash);
        assertThat(new BCryptPasswordEncoder(12).matches(NEW_PASS, newHash)).isTrue();

        // Phiên hiện tại vẫn còn hiệu lực
        assertNull(jdbc.queryForObject("SELECT revoked_at FROM auth_sessions WHERE id=?", Timestamp.class, currentSession));
        // Phiên khác đã bị thu hồi
        assertNotNull(jdbc.queryForObject("SELECT revoked_at FROM auth_sessions WHERE id=?", Timestamp.class, otherSession));

        // Audit log đã được ghi
        Integer auditCount = jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE actor_user_id=? AND action='PASSWORD_CHANGED'", Integer.class, userId);
        assertEquals(1, auditCount);
    }

    @Test
    @DisplayName("Sai mật khẩu hiện tại: trả về 400 CURRENT_PASSWORD_INCORRECT, không trả 401")
    void shouldFailWhenCurrentPasswordIsIncorrect() throws Exception {
        UUID userId = createUser("user-wrong-pwd@example.test", OLD_PASS);

        String requestBody = "{\"currentPassword\":\"WrongPassword123!\",\"newPassword\":\"" + NEW_PASS + "\"}";

        mvc.perform(post("/api/v1/me/password")
                        .with(jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.detail").value("CURRENT_PASSWORD_INCORRECT"));
    }

    @Test
    @DisplayName("Mật khẩu mới trùng với mật khẩu cũ: trả về 400 NEW_PASSWORD_SAME_AS_OLD")
    void shouldFailWhenNewPasswordSameAsOld() throws Exception {
        UUID userId = createUser("user-same-pwd@example.test", OLD_PASS);

        String requestBody = "{\"currentPassword\":\"" + OLD_PASS + "\",\"newPassword\":\"" + OLD_PASS + "\"}";

        mvc.perform(post("/api/v1/me/password")
                        .with(jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.detail").value("NEW_PASSWORD_SAME_AS_OLD"));
    }

    @Test
    @DisplayName("Mật khẩu mới chứa phần email của user: trả về 400 PASSWORD_CONTAINS_USER_INFO")
    void shouldFailWhenNewPasswordContainsEmail() throws Exception {
        String email = "johnwick@example.test";
        UUID userId = createUser(email, OLD_PASS);

        String requestBody = "{\"currentPassword\":\"" + OLD_PASS + "\",\"newPassword\":\"Secretjohnwick123!\"}";

        mvc.perform(post("/api/v1/me/password")
                        .with(jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.detail").value("PASSWORD_CONTAINS_USER_INFO"));
    }

    @Test
    @DisplayName("Mật khẩu mới ngắn hơn 12 ký tự: trả về 400 PASSWORD_LENGTH_INVALID")
    void shouldFailWhenNewPasswordTooShort() throws Exception {
        UUID userId = createUser("user-short-pwd@example.test", OLD_PASS);

        String requestBody = "{\"currentPassword\":\"" + OLD_PASS + "\",\"newPassword\":\"Short1!\"}";

        mvc.perform(post("/api/v1/me/password")
                        .with(jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.detail").value("PASSWORD_LENGTH_INVALID"));
    }

    @Test
    @DisplayName("Tài khoản Google-only không có mật khẩu: trả về 400 OAUTH_ONLY_ACCOUNT và canChangePassword=false trong profile")
    void shouldRejectGoogleOnlyUserFromChangingPassword() throws Exception {
        UUID id = UUID.randomUUID();
        String email = "google-only-" + id + "@example.test";
        var encrypted = cipher.encrypt("users.email", id, id, email);
        jdbc.update("""
                INSERT INTO users(id, email_lookup_hash, encrypted_email, email_iv, email_wrapped_key, email_key_version,
                                  password_hash, auth_provider, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, NULL, 'GOOGLE', 'ACTIVE', ?, ?)
                """, id, cipher.lookupHash(email), encrypted.ciphertext(), encrypted.iv(), encrypted.wrappedKey(),
                encrypted.keyVersion(), Timestamp.from(NOW), Timestamp.from(NOW));
        profiles.createDefault(id, "Asia/Ho_Chi_Minh", "vi", "v1", "v1", NOW);

        // Kiểm tra GET /api/v1/me có canChangePassword = false
        mvc.perform(get("/api/v1/me")
                        .with(jwt().jwt(j -> j.subject(id.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canChangePassword").value(false));

        // Gọi POST /api/v1/me/password bị từ chối
        String requestBody = "{\"currentPassword\":\"WhateverPassword!\",\"newPassword\":\"" + NEW_PASS + "\"}";
        mvc.perform(post("/api/v1/me/password")
                        .with(jwt().jwt(j -> j.subject(id.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.detail").value("OAUTH_ONLY_ACCOUNT"));
    }

    @Test
    @DisplayName("Rate limiting: Thử sai 5 lần liên tiếp -> Lần thứ 6 trả về 429 kèm Retry-After: 900")
    void shouldTriggerRateLimitAfter5FailedAttempts() throws Exception {
        UUID userId = createUser("user-ratelimit@example.test", OLD_PASS);
        String requestBody = "{\"currentPassword\":\"IncorrectPassword!\",\"newPassword\":\"" + NEW_PASS + "\"}";

        // 5 lần thử sai đầu tiên đều nhận 400
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/v1/me/password")
                            .with(jwt().jwt(j -> j.subject(userId.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.detail").value("CURRENT_PASSWORD_INCORRECT"));
        }

        // Lần thứ 6 bị chặn 429
        mvc.perform(post("/api/v1/me/password")
                        .with(jwt().jwt(j -> j.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "900"));
    }

    @Test
    @DisplayName("Chưa đăng nhập gọi endpoint: trả về 401 Unauthorized")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        String requestBody = "{\"currentPassword\":\"Whatever\",\"newPassword\":\"Whatever\"}";
        mvc.perform(post("/api/v1/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Bảo mật: toString() của ChangePasswordRequest không in mật khẩu ra chuỗi")
    void requestToStringShouldMaskPasswords() {
        ChangePasswordRequest request = new ChangePasswordRequest("PlainCurrentPassword123!", "PlainNewPassword456!");
        String stringVal = request.toString();
        assertThat(stringVal).doesNotContain("PlainCurrentPassword123!");
        assertThat(stringVal).doesNotContain("PlainNewPassword456!");
        assertThat(stringVal).contains("***");
    }

    private UUID createUser(String email, String password) {
        UUID id = UUID.randomUUID();
        var encrypted = cipher.encrypt("users.email", id, id, email);
        jdbc.update("""
                INSERT INTO users(id, email_lookup_hash, encrypted_email, email_iv, email_wrapped_key, email_key_version,
                                  password_hash, auth_provider, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'LOCAL', 'ACTIVE', ?, ?)
                """, id, cipher.lookupHash(email), encrypted.ciphertext(), encrypted.iv(), encrypted.wrappedKey(),
                encrypted.keyVersion(), new BCryptPasswordEncoder(12).encode(password),
                Timestamp.from(NOW), Timestamp.from(NOW));
        profiles.createDefault(id, "Asia/Ho_Chi_Minh", "vi", "v1", "v1", NOW);
        return id;
    }

    private UUID createSession(UUID userId, String deviceName) {
        UUID sessionId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        byte[] tokenHash = cipher.tokenHash("token-" + sessionId);
        jdbc.update("""
                INSERT INTO auth_sessions(id, user_id, token_family_id, current_token_hash, device_name,
                                          last_used_at, expires_at, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, sessionId, userId, familyId, tokenHash, deviceName,
                Timestamp.from(NOW), Timestamp.from(NOW.plusSeconds(86400 * 30)), Timestamp.from(NOW));
        return sessionId;
    }
}
