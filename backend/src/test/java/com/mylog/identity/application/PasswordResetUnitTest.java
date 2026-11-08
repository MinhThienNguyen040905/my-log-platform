package com.mylog.identity.application;

import com.mylog.platform.config.MylogProperties;
import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.platform.web.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordResetUnitTest {

    static class FakeIdentityStore implements IdentityStore {
        Account account;
        String updatedPasswordHash;
        boolean allSessionsRevoked;
        boolean emailVerified;
        int actionTokenCreatedCount;
        int existingTokensRevokedCount;
        UUID lastActionTokenUserId;
        String lastActionTokenPurpose;
        boolean rateLimitExceeded = false;
        String auditAction;
        UUID consumedTokenUserId;
        UUID consumedCodeUserId;
        int actionCodeFailureCount;

        @Override public void createAccount(Account account, Instant now) {}
        @Override public void savePendingRegistration(PendingRegistration registration, Instant now) {}
        @Override public Optional<PendingRegistration> pendingByEmailHashForUpdate(byte[] hash) { return Optional.empty(); }
        @Override public Optional<PendingRegistration> pendingByTokenHashForUpdate(byte[] hash) { return Optional.empty(); }
        @Override public void updatePendingChallenge(UUID id, byte[] tokenHash, byte[] codeHash, Instant expiresAt, Instant codeExpiresAt, Instant now) {}
        @Override public void recordPendingCodeFailure(UUID id) {}
        @Override public void deletePendingRegistration(UUID id) {}
        @Override public Optional<Account> accountByEmailHash(byte[] hash) { return Optional.ofNullable(account); }
        @Override public Optional<Account> accountByEmailHashForUpdate(byte[] hash) { return Optional.ofNullable(account); }
        @Override public Optional<Account> accountById(UUID id) { return Optional.ofNullable(account); }
        @Override public void setLoginFailure(UUID userId, int failures, Instant lockedUntil, Instant now) {}
        @Override public void setLoginSuccess(UUID userId, Instant now) {}
        @Override public void setStatus(UUID userId, String status, Instant now) {}
        @Override public void changePassword(UUID userId, String hash, Instant now) { this.updatedPasswordHash = hash; }
        @Override public void clearPassword(UUID userId, Instant now) {}
        @Override public void assignUserRole(UUID userId, Instant now) {}
        @Override public List<String> roles(UUID userId) { return List.of("USER"); }
        @Override public List<String> permissions(UUID userId) { return List.of(); }
        @Override public void createSession(Session session) {}
        @Override public Optional<Session> sessionForCurrentToken(byte[] tokenHash) { return Optional.empty(); }
        @Override public Optional<Session> sessionForUsedToken(byte[] tokenHash) { return Optional.empty(); }
        @Override public Optional<Session> sessionById(UUID sessionId) { return Optional.empty(); }
        @Override public List<Session> sessions(UUID userId) { return List.of(); }
        @Override public List<Session> allSessions(UUID userId) { return List.of(); }
        @Override public void rotate(UUID sessionId, byte[] oldHash, byte[] newHash, Instant expiresAt, Instant now) {}
        @Override public void revokeFamily(UUID familyId, String reason, Instant now) {}
        @Override public void revokeSession(UUID userId, UUID sessionId, String reason, Instant now) {}
        @Override public void revokeOtherSessions(UUID userId, UUID except, Instant now) {}
        @Override public void revokeAllSessions(UUID userId, String reason, Instant now) { this.allSessionsRevoked = true; }
        @Override public void createActionToken(UUID id, UUID userId, byte[] hash, byte[] codeHash, String purpose, Instant expiresAt, Instant codeExpiresAt, Instant now) {
            this.actionTokenCreatedCount++;
            this.lastActionTokenUserId = userId;
            this.lastActionTokenPurpose = purpose;
        }
        @Override public void revokeExistingActionTokens(UUID userId, String purpose, Instant now) {
            this.existingTokensRevokedCount++;
        }
        @Override public Optional<UUID> consumeActionToken(byte[] hash, String purpose, Instant now) {
            return Optional.ofNullable(consumedTokenUserId);
        }
        @Override public Optional<UUID> consumeActionCode(UUID userId, byte[] codeHash, String purpose, Instant now) {
            return Optional.ofNullable(consumedCodeUserId);
        }
        @Override public void recordActionCodeFailure(UUID userId, String purpose, Instant now) {
            this.actionCodeFailureCount++;
        }
        @Override public boolean actionTokenAlreadyConsumed(byte[] hash, String purpose) { return false; }
        @Override public void setEmailVerified(UUID userId, Instant now) { this.emailVerified = true; }
        @Override public boolean recordRateLimit(byte[] subjectHash, Instant now, int maxAttempts, long windowSeconds) {
            return !rateLimitExceeded;
        }
        @Override public void audit(UUID actorId, String action, UUID targetId, Instant now) { this.auditAction = action; }
        @Override public void purgeExpired(Instant now) {}
        @Override public Optional<UUID> findUserIdByOAuth(String provider, String providerUserId) { return Optional.empty(); }
        @Override public void linkOAuthIdentity(UUID userId, String provider, String providerUserId, Instant now) {}
        @Override public void createOAuthAccount(Account account, String provider, String providerUserId, Instant now) {}
        @Override public void reclaimUnverifiedAccountForOAuth(UUID userId, String provider, Instant now) {}
    }

    static class FakeCipher implements SensitiveDataCipher {
        @Override public Encrypted encrypt(String table, UUID owner, UUID row, String plaintext) {
            return new Encrypted(plaintext.getBytes(), new byte[12], new byte[32], "v1");
        }
        @Override public String decrypt(String table, UUID owner, UUID row, Encrypted encrypted) {
            return new String(encrypted.ciphertext());
        }
        @Override public byte[] lookupHash(String normalizedValue) { return normalizedValue.getBytes(); }
        @Override public byte[] tokenHash(String token) { return token.getBytes(); }
    }

    static class FakeEventPublisher implements ApplicationEventPublisher {
        final List<Object> publishedEvents = new ArrayList<>();
        @Override public void publishEvent(Object event) { publishedEvents.add(event); }
    }

    private FakeIdentityStore store;
    private FakeCipher cipher;
    private FakeEventPublisher eventPublisher;
    private IdentityService identityService;
    private UUID testUserId;
    private String testEmail;
    private String currentPasswordHash;

    @BeforeEach
    void setUp() {
        store = new FakeIdentityStore();
        cipher = new FakeCipher();
        eventPublisher = new FakeEventPublisher();
        testUserId = UUID.randomUUID();
        testEmail = "user@mylog.vn";
        currentPasswordHash = new BCryptPasswordEncoder(12).encode("OldPassword123!");

        store.account = new IdentityStore.Account(
                testUserId,
                cipher.lookupHash(testEmail),
                cipher.encrypt("users.email", testUserId, testUserId, testEmail),
                currentPasswordHash,
                "ACTIVE",
                0,
                null,
                1L,
                null // initially unverified
        );

        MylogProperties props = new MylogProperties(
                "all",
                new MylogProperties.Web(List.of("http://localhost:3000")),
                new MylogProperties.OpenApi(false),
                new MylogProperties.Identity(true, new MylogProperties.Identity.OAuth(
                        new MylogProperties.Identity.OAuth.Google(false, null, null)
                ))
        );
        Clock clock = Clock.fixed(Instant.parse("2026-03-31T10:00:00Z"), ZoneOffset.UTC);

        identityService = new IdentityService(
                store, cipher, (u, s) -> "jwt", null, null,
                UUID::randomUUID, clock, new SecureRandom(),
                null, null, props, null, eventPublisher
        );
    }

    @Test
    @DisplayName("requestPasswordReset - Thành công với email tồn tại: hủy token cũ, tạo token mới, bắn event")
    void requestPasswordReset_success() {
        identityService.requestPasswordReset("user@mylog.vn", "127.0.0.1");

        assertThat(store.existingTokensRevokedCount).isEqualTo(1);
        assertThat(store.actionTokenCreatedCount).isEqualTo(1);
        assertThat(store.lastActionTokenUserId).isEqualTo(testUserId);
        assertThat(store.lastActionTokenPurpose).isEqualTo("RESET_PASSWORD");
        assertThat(eventPublisher.publishedEvents).hasSize(1);
        assertThat(eventPublisher.publishedEvents.get(0)).isInstanceOf(PasswordResetRequestedEvent.class);
    }

    @Test
    @DisplayName("requestPasswordReset - Anti-enumeration: email không tồn tại không ném lỗi và không tạo token")
    void requestPasswordReset_nonExistentEmail_doesNotLeak() {
        store.account = null;

        identityService.requestPasswordReset("nonexistent@mylog.vn", "127.0.0.1");

        assertThat(store.actionTokenCreatedCount).isEqualTo(0);
        assertThat(eventPublisher.publishedEvents).isEmpty();
    }

    @Test
    @DisplayName("requestPasswordReset - Tài khoản OAuth-only gửi event thông báo riêng, không tạo reset token")
    void requestPasswordReset_oauthOnlyAccount() {
        store.account = new IdentityStore.Account(
                testUserId, cipher.lookupHash(testEmail),
                cipher.encrypt("users.email", testUserId, testUserId, testEmail),
                null, // no password
                "ACTIVE", 0, null, 1L, Instant.now()
        );

        identityService.requestPasswordReset("user@mylog.vn", "127.0.0.1");

        assertThat(store.actionTokenCreatedCount).isEqualTo(0);
        assertThat(eventPublisher.publishedEvents).hasSize(1);
        assertThat(eventPublisher.publishedEvents.get(0)).isInstanceOf(OAuthPasswordResetAttemptEvent.class);
    }

    @Test
    @DisplayName("requestPasswordReset - Vượt rate limit ném RateLimitExceededException")
    void requestPasswordReset_rateLimited() {
        store.rateLimitExceeded = true;

        assertThatThrownBy(() -> identityService.requestPasswordReset("user@mylog.vn", "127.0.0.1"))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @DisplayName("resetPasswordWithToken - Thành công: đổi mật khẩu, auto-verify email, thu hồi sessions")
    void resetPasswordWithToken_success() {
        store.consumedTokenUserId = testUserId;

        identityService.resetPasswordWithToken("valid_token", "BrandNewPassword123!", "127.0.0.1");

        assertThat(store.updatedPasswordHash).isNotNull();
        assertThat(new BCryptPasswordEncoder(12).matches("BrandNewPassword123!", store.updatedPasswordHash)).isTrue();
        assertThat(store.emailVerified).isTrue();
        assertThat(store.allSessionsRevoked).isTrue();
        assertThat(store.auditAction).isEqualTo("PASSWORD_RESET");
        assertThat(eventPublisher.publishedEvents).hasSize(1);
        assertThat(eventPublisher.publishedEvents.get(0)).isInstanceOf(PasswordChangedEvent.class);
    }

    @Test
    @DisplayName("resetPasswordWithToken - Mật khẩu trùng mật khẩu cũ ném NEW_PASSWORD_SAME_AS_OLD")
    void resetPasswordWithToken_sameAsOldPassword() {
        store.consumedTokenUserId = testUserId;

        assertThatThrownBy(() -> identityService.resetPasswordWithToken("valid_token", "OldPassword123!", "127.0.0.1"))
                .isInstanceOf(PasswordChangeException.class)
                .extracting("safeDetail")
                .isEqualTo("NEW_PASSWORD_SAME_AS_OLD");
    }

    @Test
    @DisplayName("resetPasswordWithToken - Mật khẩu chứa username ném PASSWORD_CONTAINS_USER_INFO")
    void resetPasswordWithToken_containsUsername() {
        store.consumedTokenUserId = testUserId;

        assertThatThrownBy(() -> identityService.resetPasswordWithToken("valid_token", "UserPassword123!", "127.0.0.1"))
                .isInstanceOf(PasswordChangeException.class)
                .extracting("safeDetail")
                .isEqualTo("PASSWORD_CONTAINS_USER_INFO");
    }

    @Test
    @DisplayName("resetPasswordWithToken - Mật khẩu dưới 12 ký tự ném PASSWORD_LENGTH_INVALID")
    void resetPasswordWithToken_tooShort() {
        store.consumedTokenUserId = testUserId;

        assertThatThrownBy(() -> identityService.resetPasswordWithToken("valid_token", "Short1!", "127.0.0.1"))
                .isInstanceOf(PasswordChangeException.class)
                .extracting("safeDetail")
                .isEqualTo("PASSWORD_LENGTH_INVALID");
    }

    @Test
    @DisplayName("resetPasswordWithToken - Mật khẩu > 72 bytes UTF-8 ném PASSWORD_LENGTH_INVALID")
    void resetPasswordWithToken_tooLongUtf8Bytes() {
        store.consumedTokenUserId = testUserId;
        String longVietnamese = "TiếngViệtRấtĐẹpVàPhongPhúCựcKỳTuyệtVời123!".repeat(2);
        assertThat(longVietnamese.getBytes(java.nio.charset.StandardCharsets.UTF_8).length).isGreaterThan(72);

        assertThatThrownBy(() -> identityService.resetPasswordWithToken("valid_token", longVietnamese, "127.0.0.1"))
                .isInstanceOf(PasswordChangeException.class)
                .extracting("safeDetail")
                .isEqualTo("PASSWORD_LENGTH_INVALID");
    }

    @Test
    @DisplayName("resetPasswordWithCode - Thành công với mã OTP đúng")
    void resetPasswordWithCode_success() {
        store.consumedCodeUserId = testUserId;

        identityService.resetPasswordWithCode("user@mylog.vn", "12345678", "BrandNewPassword123!", "127.0.0.1");

        assertThat(store.updatedPasswordHash).isNotNull();
        assertThat(store.allSessionsRevoked).isTrue();
        assertThat(store.emailVerified).isTrue();
        assertThat(store.auditAction).isEqualTo("PASSWORD_RESET");
    }

    @Test
    @DisplayName("resetPasswordWithCode - OTP sai ném InvalidCredentialsException")
    void resetPasswordWithCode_wrongCode() {
        store.consumedCodeUserId = null; // code consumption failed

        assertThatThrownBy(() -> identityService.resetPasswordWithCode("user@mylog.vn", "99999999", "BrandNewPassword123!", "127.0.0.1"))
                .isInstanceOf(IdentityService.InvalidCredentialsException.class);
    }
}
