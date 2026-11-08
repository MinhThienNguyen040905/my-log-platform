package com.mylog.user;

import com.mylog.identity.application.IdentityService;
import com.mylog.identity.application.IdentityStore;
import com.mylog.identity.application.PasswordChangeException;
import com.mylog.identity.application.RateLimitService;
import com.mylog.platform.config.MylogProperties;
import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.platform.security.UnauthenticatedUserException;
import com.mylog.platform.web.RateLimitExceededException;
import com.mylog.user.api.request.ChangePasswordRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

class ChangePasswordUnitTest {

    static class FakeIdentityStore implements IdentityStore {
        Account account;
        String updatedPasswordHash;
        UUID revokedOtherExcept;
        boolean allSessionsRevoked;
        boolean rateLimitCleared;
        boolean rateLimited;
        boolean sessionBelongs = true;
        int recordedRateLimitCount;
        String auditAction;

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
        @Override public void revokeOtherSessions(UUID userId, UUID except, Instant now) { this.revokedOtherExcept = except; }
        @Override public void revokeAllSessions(UUID userId, String reason, Instant now) { this.allSessionsRevoked = true; }
        @Override public void createActionToken(UUID id, UUID userId, byte[] hash, byte[] codeHash, String purpose, Instant expiresAt, Instant codeExpiresAt, Instant now) {}
        @Override public Optional<UUID> consumeActionToken(byte[] hash, String purpose, Instant now) { return Optional.empty(); }
        @Override public boolean actionTokenAlreadyConsumed(byte[] hash, String purpose) { return false; }
        @Override public boolean recordRateLimit(byte[] subjectHash, Instant now, int maxAttempts, long windowSeconds) {
            recordedRateLimitCount++;
            return true;
        }
        @Override public boolean isRateLimited(byte[] subjectHash, Instant now, int maxAttempts, long windowSeconds) { return rateLimited; }
        @Override public void clearRateLimit(byte[] subjectHash) { this.rateLimitCleared = true; }
        @Override public boolean sessionBelongsToUser(UUID sessionId, UUID userId) { return sessionBelongs; }
        @Override public boolean hasPassword(UUID userId) { return account != null && account.passwordHash() != null; }
        @Override public void audit(UUID actorId, String action, UUID targetId, Instant now) { this.auditAction = action; }
        @Override public void purgeExpired(Instant now) {}
        @Override public Optional<UUID> findUserIdByOAuth(String provider, String providerUserId) { return Optional.empty(); }
        @Override public void linkOAuthIdentity(UUID userId, String provider, String providerUserId, Instant now) {}
        @Override public void createOAuthAccount(Account account, String provider, String providerUserId, Instant now) {}
        @Override public void reclaimUnverifiedAccountForOAuth(UUID userId, String provider, Instant now) {}
    }

    static class FakeSensitiveDataCipher implements SensitiveDataCipher {
        @Override public byte[] lookupHash(String plaintext) {
            return plaintext.getBytes();
        }
        @Override public byte[] tokenHash(String token) {
            return token.getBytes();
        }
        @Override public Encrypted encrypt(String keyPurpose, UUID subjectId, UUID resourceId, String plaintext) {
            return new Encrypted(plaintext.getBytes(), new byte[]{1}, new byte[]{2}, "v1");
        }
        @Override public String decrypt(String keyPurpose, UUID subjectId, UUID resourceId, Encrypted encrypted) {
            return new String(encrypted.ciphertext());
        }
    }

    private FakeIdentityStore store;
    private SensitiveDataCipher cipher;
    private IdentityService service;
    private RateLimitService rateLimitService;
    private List<Object> publishedEvents;
    private static final String OLD_PASS = "CurrentCorrectPass123!";
    private static final String NEW_PASS = "NewBrandSuperSecure456!";
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SESSION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        store = new FakeIdentityStore();
        cipher = new FakeSensitiveDataCipher();
        rateLimitService = new RateLimitService(store);
        publishedEvents = new ArrayList<>();

        MylogProperties props = new MylogProperties(
                "all",
                new MylogProperties.Web(List.of("http://localhost:3000")),
                new MylogProperties.OpenApi(false),
                new MylogProperties.Identity(true, new MylogProperties.Identity.OAuth(
                        new MylogProperties.Identity.OAuth.Google(false, null, null)
                ))
        );
        Clock clock = Clock.fixed(Instant.parse("2026-10-10T00:00:00Z"), ZoneOffset.UTC);

        service = new IdentityService(
                store, cipher, (u, s) -> "jwt", null, null,
                UUID::randomUUID, clock, new SecureRandom(), null, null, props,
                rateLimitService, publishedEvents::add
        );

        var encryptedEmail = cipher.encrypt("users.email", USER_ID, USER_ID, "user-test@example.com");
        store.account = new IdentityStore.Account(
                USER_ID, cipher.lookupHash("user-test@example.com"), encryptedEmail,
                new BCryptPasswordEncoder(12).encode(OLD_PASS), "ACTIVE", 0, null, 1, Instant.now()
        );
    }

    @Test
    @DisplayName("Đổi mật khẩu thành công: mã hóa BCrypt, thu hồi các session khác, reset rate limit, phát event")
    void shouldChangePasswordSuccessfully() {
        ChangePasswordRequest request = new ChangePasswordRequest(OLD_PASS, NEW_PASS);
        service.changePassword(USER_ID, SESSION_ID, request, "127.0.0.1");

        assertThat(store.updatedPasswordHash).isNotNull();
        assertThat(new BCryptPasswordEncoder(12).matches(NEW_PASS, store.updatedPasswordHash)).isTrue();
        assertThat(store.revokedOtherExcept).isEqualTo(SESSION_ID);
        assertThat(store.rateLimitCleared).isTrue();
        assertThat(store.auditAction).isEqualTo("PASSWORD_CHANGED");
        assertThat(publishedEvents).hasSize(1);
    }

    @Test
    @DisplayName("Sai mật khẩu cũ: ghi nhận rate limit, ném lỗi CURRENT_PASSWORD_INCORRECT")
    void shouldFailWhenCurrentPasswordIsIncorrect() {
        ChangePasswordRequest request = new ChangePasswordRequest("WrongPassword123!", NEW_PASS);

        assertThatThrownBy(() -> service.changePassword(USER_ID, SESSION_ID, request, "127.0.0.1"))
                .isInstanceOf(PasswordChangeException.class)
                .hasMessage("VALIDATION_ERROR")
                .extracting("safeDetail")
                .isEqualTo("CURRENT_PASSWORD_INCORRECT");

        assertThat(store.recordedRateLimitCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Mật khẩu mới trùng với mật khẩu cũ: ném lỗi NEW_PASSWORD_SAME_AS_OLD")
    void shouldFailWhenNewPasswordSameAsOld() {
        ChangePasswordRequest request = new ChangePasswordRequest(OLD_PASS, OLD_PASS);

        assertThatThrownBy(() -> service.changePassword(USER_ID, SESSION_ID, request, "127.0.0.1"))
                .isInstanceOf(PasswordChangeException.class)
                .extracting("safeDetail")
                .isEqualTo("NEW_PASSWORD_SAME_AS_OLD");
    }

    @Test
    @DisplayName("Mật khẩu mới chứa phần email: ném lỗi PASSWORD_CONTAINS_USER_INFO")
    void shouldFailWhenNewPasswordContainsEmail() {
        ChangePasswordRequest request = new ChangePasswordRequest(OLD_PASS, "SuperSecretuser-test123!");

        assertThatThrownBy(() -> service.changePassword(USER_ID, SESSION_ID, request, "127.0.0.1"))
                .isInstanceOf(PasswordChangeException.class)
                .extracting("safeDetail")
                .isEqualTo("PASSWORD_CONTAINS_USER_INFO");
    }

    @Test
    @DisplayName("Mật khẩu mới ngắn hơn 12 ký tự: ném lỗi PASSWORD_LENGTH_INVALID")
    void shouldFailWhenNewPasswordTooShort() {
        ChangePasswordRequest request = new ChangePasswordRequest(OLD_PASS, "Short123!");

        assertThatThrownBy(() -> service.changePassword(USER_ID, SESSION_ID, request, "127.0.0.1"))
                .isInstanceOf(PasswordChangeException.class)
                .extracting("safeDetail")
                .isEqualTo("PASSWORD_LENGTH_INVALID");
    }

    @Test
    @DisplayName("Tài khoản Google-only (không có mật khẩu): ném lỗi OAUTH_ONLY_ACCOUNT")
    void shouldRejectGoogleOnlyUser() {
        var encryptedEmail = cipher.encrypt("users.email", USER_ID, USER_ID, "google@example.com");
        store.account = new IdentityStore.Account(
                USER_ID, cipher.lookupHash("google@example.com"), encryptedEmail,
                null, "ACTIVE", 0, null, 1, Instant.now()
        );

        ChangePasswordRequest request = new ChangePasswordRequest("AnyPassword123!", NEW_PASS);

        assertThatThrownBy(() -> service.changePassword(USER_ID, SESSION_ID, request, "127.0.0.1"))
                .isInstanceOf(PasswordChangeException.class)
                .extracting("safeDetail")
                .isEqualTo("OAUTH_ONLY_ACCOUNT");
    }

    @Test
    @DisplayName("Rate limit kích hoạt: ném lỗi RateLimitExceededException khi thử quá 5 lần")
    void shouldBlockWhenRateLimited() {
        store.rateLimited = true;
        ChangePasswordRequest request = new ChangePasswordRequest(OLD_PASS, NEW_PASS);

        assertThatThrownBy(() -> service.changePassword(USER_ID, SESSION_ID, request, "127.0.0.1"))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @DisplayName("Không tìm thấy account: ném UnauthenticatedUserException")
    void shouldThrowWhenAccountNotFound() {
        store.account = null;
        ChangePasswordRequest request = new ChangePasswordRequest(OLD_PASS, NEW_PASS);

        assertThatThrownBy(() -> service.changePassword(USER_ID, SESSION_ID, request, "127.0.0.1"))
                .isInstanceOf(UnauthenticatedUserException.class);
    }
}
