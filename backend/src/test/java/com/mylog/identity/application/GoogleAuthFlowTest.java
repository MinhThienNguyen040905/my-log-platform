package com.mylog.identity.application;

import com.mylog.identity.api.request.GoogleAuthRequest;
import com.mylog.identity.application.result.AuthResult;
import com.mylog.identity.infrastructure.GoogleOAuthClient;
import com.mylog.identity.infrastructure.GoogleOAuthClient.GoogleTokenPayload;
import com.mylog.platform.config.MylogProperties;
import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.platform.web.ResourceNotFoundException;
import com.mylog.user.application.UserProfileUseCase;
import com.mylog.user.application.query.ConsentView;
import com.mylog.user.application.query.ProfileView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleAuthFlowTest {

    static class FakeIdentityStore implements IdentityStore {
        Account createdAccount;
        String createdProvider;
        String createdProviderUserId;
        boolean userRoleAssigned;
        boolean reclaimedUnverified;
        boolean allSessionsRevoked;
        boolean linkedOAuth;
        String auditAction;

        Optional<UUID> oauthUserId = Optional.empty();
        Optional<Account> accountByEmail = Optional.empty();

        @Override public void createAccount(Account account, Instant now) { createdAccount = account; }
        @Override public void savePendingRegistration(PendingRegistration registration, Instant now) {}
        @Override public Optional<PendingRegistration> pendingByEmailHashForUpdate(byte[] hash) { return Optional.empty(); }
        @Override public Optional<PendingRegistration> pendingByTokenHashForUpdate(byte[] hash) { return Optional.empty(); }
        @Override public void updatePendingChallenge(UUID id, byte[] tokenHash, byte[] codeHash, Instant expiresAt, Instant codeExpiresAt, Instant now) {}
        @Override public void recordPendingCodeFailure(UUID id) {}
        @Override public void deletePendingRegistration(UUID id) {}
        @Override public Optional<Account> accountByEmailHash(byte[] hash) { return accountByEmail; }
        @Override public Optional<Account> accountByEmailHashForUpdate(byte[] hash) { return accountByEmail; }
        @Override public Optional<Account> accountById(UUID id) { return accountByEmail; }
        @Override public void setLoginFailure(UUID userId, int failures, Instant lockedUntil, Instant now) {}
        @Override public void setLoginSuccess(UUID userId, Instant now) {}
        @Override public void setStatus(UUID userId, String status, Instant now) {}
        @Override public void changePassword(UUID userId, String hash, Instant now) {}
        @Override public void clearPassword(UUID userId, Instant now) {}
        @Override public void assignUserRole(UUID userId, Instant now) { userRoleAssigned = true; }
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
        @Override public void revokeAllSessions(UUID userId, String reason, Instant now) { allSessionsRevoked = true; }
        @Override public void createActionToken(UUID id, UUID userId, byte[] hash, byte[] codeHash, String purpose, Instant expiresAt, Instant codeExpiresAt, Instant now) {}
        @Override public Optional<UUID> consumeActionToken(byte[] hash, String purpose, Instant now) { return Optional.empty(); }
        @Override public boolean actionTokenAlreadyConsumed(byte[] hash, String purpose) { return false; }
        @Override public boolean recordRateLimit(byte[] subjectHash, Instant now, int maxAttempts, long windowSeconds) { return true; }
        @Override public void audit(UUID actorId, String action, UUID targetId, Instant now) { auditAction = action; }
        @Override public void purgeExpired(Instant now) {}

        @Override public Optional<UUID> findUserIdByOAuth(String provider, String providerUserId) { return oauthUserId; }
        @Override public void linkOAuthIdentity(UUID userId, String provider, String providerUserId, Instant now) { linkedOAuth = true; }
        @Override public void createOAuthAccount(Account account, String provider, String providerUserId, Instant now) {
            createdAccount = account;
            createdProvider = provider;
            createdProviderUserId = providerUserId;
        }
        @Override public void reclaimUnverifiedAccountForOAuth(UUID userId, String provider, Instant now) { reclaimedUnverified = true; }
    }

    static class FakeSensitiveDataCipher implements SensitiveDataCipher {
        @Override public byte[] lookupHash(String plaintext) { return plaintext.getBytes(); }
        @Override public byte[] tokenHash(String plaintext) { return plaintext.getBytes(); }
        @Override public Encrypted encrypt(String keyPurpose, UUID subjectId, UUID resourceId, String plaintext) {
            return new Encrypted(plaintext.getBytes(), new byte[]{1}, new byte[]{2}, "v1");
        }
        @Override public String decrypt(String keyPurpose, UUID subjectId, UUID resourceId, Encrypted encrypted) {
            return new String(encrypted.ciphertext());
        }
    }

    private FakeIdentityStore store;
    private FakeSensitiveDataCipher cipher;
    private final Instant now = Instant.parse("2026-11-04T10:00:00Z");
    private final UUID fixedUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        store = new FakeIdentityStore();
        cipher = new FakeSensitiveDataCipher();
    }

    @Test
    @DisplayName("Ném ResourceNotFoundException nếu Google OAuth bị tắt")
    void shouldThrowIfOAuthDisabled() {
        MylogProperties disabledProps = new MylogProperties(
                "all",
                new MylogProperties.Web(List.of("http://localhost:3000")),
                new MylogProperties.OpenApi(false),
                new MylogProperties.Identity(true, new MylogProperties.Identity.OAuth(
                        new MylogProperties.Identity.OAuth.Google(false, null, null)
                ))
        );

        GoogleOAuthClient client = new GoogleOAuthClient(disabledProps);
        GoogleAuthProvisioningService provisioningService = new GoogleAuthProvisioningService(
                store, cipher, () -> fixedUserId, null, (u, s) -> "jwt"
        );
        IdentityService service = new IdentityService(
                store, cipher, (u, s) -> "jwt", null, null, () -> fixedUserId, Clock.fixed(now, ZoneOffset.UTC),
                new SecureRandom(), client, provisioningService, disabledProps, null, null
        );

        assertThatThrownBy(() -> service.loginWithGoogle(
                new GoogleAuthRequest("code", "Asia/Ho_Chi_Minh", "vi", "Web"), "127.0.0.1"
        )).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("JIT Provisioning: Tạo tài khoản mới khi đăng nhập Google lần đầu")
    void shouldProvisionNewUserOnGoogleLogin() {
        MylogProperties props = new MylogProperties(
                "all",
                new MylogProperties.Web(List.of("http://localhost:3000")),
                new MylogProperties.OpenApi(false),
                new MylogProperties.Identity(true, new MylogProperties.Identity.OAuth(
                        new MylogProperties.Identity.OAuth.Google(true, "cid", "sec")
                ))
        );

        GoogleOAuthClient mockClient = new GoogleOAuthClient(props) {
            @Override
            public GoogleTokenPayload exchangeCode(String code) {
                return new GoogleTokenPayload("google-sub-123", "newuser@example.com", true, "New User");
            }
        };

        boolean[] profileCreated = {false};
        UserProfileUseCase fakeProfiles = new UserProfileUseCase() {
            @Override public void validateRegistrationPreferences(String timezone, String locale) {}
            @Override public void createDefault(UUID userId, String timezone, String locale, String termsVersion, String privacyVersion, Instant now) {
                profileCreated[0] = true;
            }
            @Override public ProfileView get(UUID userId) { return null; }
            @Override public ProfileView update(UUID userId, String displayName, String penName, List<String> onboardingGoals, String timezone, String locale, long expectedVersion) { return null; }
            @Override public ProfileView completeOnboarding(UUID userId, long expectedVersion) { return null; }
            @Override public void decideConsent(UUID userId, String type, String documentVersion, boolean granted) {}
            @Override public List<ConsentView> consents(UUID userId) { return List.of(); }
            @Override public boolean isConsentGranted(UUID userId, String type) { return false; }
        };

        GoogleAuthProvisioningService provisioningService = new GoogleAuthProvisioningService(
                store, cipher, () -> fixedUserId, fakeProfiles, (u, s) -> "jwt-access-token"
        );
        IdentityService service = new IdentityService(
                store, cipher, (u, s) -> "jwt", null, null, () -> fixedUserId, Clock.fixed(now, ZoneOffset.UTC),
                new SecureRandom(), mockClient, provisioningService, props, null, null
        );

        AuthResult result = service.loginWithGoogle(
                new GoogleAuthRequest("code", "Asia/Ho_Chi_Minh", "vi", "Web"), "127.0.0.1"
        );

        assertThat(result.userId()).isEqualTo(fixedUserId);
        assertThat(result.accessToken()).isEqualTo("jwt-access-token");
        assertThat(store.createdProvider).isEqualTo("GOOGLE");
        assertThat(store.createdProviderUserId).isEqualTo("google-sub-123");
        assertThat(store.userRoleAssigned).isTrue();
        assertThat(profileCreated[0]).isTrue();
        assertThat(store.auditAction).isEqualTo("REGISTER_GOOGLE");
    }

    @Test
    @DisplayName("Pre-takeover Defense: Thu hồi session, xóa mật khẩu cũ nếu tài khoản LOCAL chưa xác minh")
    void shouldReclaimUnverifiedLocalAccount() {
        MylogProperties props = new MylogProperties(
                "all",
                new MylogProperties.Web(List.of("http://localhost:3000")),
                new MylogProperties.OpenApi(false),
                new MylogProperties.Identity(true, new MylogProperties.Identity.OAuth(
                        new MylogProperties.Identity.OAuth.Google(true, "cid", "sec")
                ))
        );

        GoogleOAuthClient mockClient = new GoogleOAuthClient(props) {
            @Override
            public GoogleTokenPayload exchangeCode(String code) {
                return new GoogleTokenPayload("google-sub-456", "victim@example.com", true, "Victim");
            }
        };

        // Tài khoản tồn tại nhưng emailVerifiedAt == null
        store.accountByEmail = Optional.of(new IdentityStore.Account(
                fixedUserId, new byte[]{1},
                new SensitiveDataCipher.Encrypted(new byte[]{1}, new byte[]{2}, new byte[]{3}, "v1"),
                "attacker-password-hash", "ACTIVE", 0, null, 1, null
        ));

        GoogleAuthProvisioningService provisioningService = new GoogleAuthProvisioningService(
                store, cipher, () -> fixedUserId, null, (u, s) -> "jwt-token"
        );
        IdentityService service = new IdentityService(
                store, cipher, (u, s) -> "jwt", null, null, () -> fixedUserId, Clock.fixed(now, ZoneOffset.UTC),
                new SecureRandom(), mockClient, provisioningService, props, null, null
        );

        AuthResult result = service.loginWithGoogle(
                new GoogleAuthRequest("code", "Asia/Ho_Chi_Minh", "vi", "Web"), "127.0.0.1"
        );

        assertThat(result.userId()).isEqualTo(fixedUserId);
        assertThat(store.reclaimedUnverified).isTrue();
        assertThat(store.allSessionsRevoked).isTrue();
        assertThat(store.linkedOAuth).isTrue();
        assertThat(store.auditAction).isEqualTo("LINK_GOOGLE");
    }

    @Test
    @DisplayName("Tài khoản Google-only không có mật khẩu (password_hash = null) đăng nhập LOCAL phải ném Unauthorized không bị NPE")
    void shouldRejectLocalLoginForPasswordlessGoogleAccount() {
        MylogProperties props = new MylogProperties(
                "all",
                new MylogProperties.Web(List.of("http://localhost:3000")),
                new MylogProperties.OpenApi(false),
                new MylogProperties.Identity(true, new MylogProperties.Identity.OAuth(
                        new MylogProperties.Identity.OAuth.Google(true, "cid", "sec")
                ))
        );

        store.accountByEmail = Optional.of(new IdentityStore.Account(
                fixedUserId, new byte[]{1},
                new SensitiveDataCipher.Encrypted(new byte[]{1}, new byte[]{2}, new byte[]{3}, "v1"),
                null, "ACTIVE", 0, null, 1, now
        ));

        IdentityService service = new IdentityService(
                store, cipher, (u, s) -> "jwt", null, null, () -> fixedUserId, Clock.fixed(now, ZoneOffset.UTC),
                new SecureRandom(), null, null, props, null, null
        );

        assertThatThrownBy(() -> service.login("googleuser@example.com", "any-password", "Web", "127.0.0.1"))
                .isInstanceOf(IdentityService.InvalidCredentialsException.class);
    }
}
