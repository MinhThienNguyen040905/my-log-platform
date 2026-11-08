package com.mylog.identity.application;

import com.mylog.identity.application.IdentityStore.Account;
import com.mylog.identity.application.IdentityStore.PendingRegistration;
import com.mylog.identity.application.IdentityStore.Session;
import com.mylog.identity.application.query.SessionSummary;
import com.mylog.identity.application.result.AuthResult;
import com.mylog.platform.id.IdGenerator;
import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.platform.web.ApplicationException;
import com.mylog.platform.web.ConflictException;
import com.mylog.platform.web.ErrorCode;
import com.mylog.platform.web.RateLimitExceededException;
import com.mylog.platform.web.ResourceNotFoundException;
import com.mylog.identity.api.request.GoogleAuthRequest;
import com.mylog.identity.infrastructure.GoogleOAuthClient;
import com.mylog.platform.config.MylogProperties;
import com.mylog.platform.web.InvalidRequestException;
import com.mylog.user.application.UserProfileUseCase;
import com.mylog.platform.security.UnauthenticatedUserException;
import com.mylog.user.api.request.ChangePasswordRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.IDN;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class IdentityService {
    private static final String DUMMY_HASH = new BCryptPasswordEncoder(12).encode("dummy-password-never-used");
    private static final byte[] DUMMY_CODE_HASH = new byte[32];
    private static final UUID DUMMY_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private final IdentityStore store;
    private final SensitiveDataCipher cipher;
    private final AccessTokens accessTokens;
    private final VerificationDelivery delivery;
    private final UserProfileUseCase profiles;
    private final IdGenerator ids;
    private final Clock clock;
    private final SecureRandom random;
    private final PasswordEncoder passwords = new BCryptPasswordEncoder(12);
    private static final Logger log = LoggerFactory.getLogger(IdentityService.class);
    private final GoogleOAuthClient googleClient;
    private final GoogleAuthProvisioningService provisioningService;
    private final MylogProperties properties;
    private final RateLimitService rateLimitService;
    private final ApplicationEventPublisher eventPublisher;
    @Autowired
    public IdentityService(IdentityStore store, SensitiveDataCipher cipher, AccessTokens accessTokens,
            VerificationDelivery delivery, UserProfileUseCase profiles, IdGenerator ids, Clock clock, SecureRandom random,
            GoogleOAuthClient googleClient, GoogleAuthProvisioningService provisioningService, MylogProperties properties,
            RateLimitService rateLimitService, ApplicationEventPublisher eventPublisher) {
        this.store = store; this.cipher = cipher; this.accessTokens = accessTokens; this.delivery = delivery;
        this.profiles = profiles; this.ids = ids; this.clock = clock; this.random = random;
        this.googleClient = googleClient; this.provisioningService = provisioningService; this.properties = properties;
        this.rateLimitService = rateLimitService; this.eventPublisher = eventPublisher;
    }

    public static String normalizeEmail(String value) {
        if (value == null) throw new InvalidRequestException();
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        int at = normalized.lastIndexOf('@');
        if (at < 1 || at == normalized.length() - 1 || normalized.length() > 254) throw new InvalidRequestException();
        String domain;
        try { domain = IDN.toASCII(normalized.substring(at + 1)); }
        catch (IllegalArgumentException e) { throw new InvalidRequestException(); }
        return normalized.substring(0, at) + "@" + domain;
    }

    @Transactional
    public UUID register(String email, String password, String timezone, String locale,
                         String termsVersion, String privacyVersion, String remoteAddress) {
        Instant now = clock.instant();
        String normalized = normalizeEmail(email);
        rate("register:" + remoteAddress, now, 20);
        rate("register-email:" + normalized, now, 5);
        profiles.validateRegistrationPreferences(timezone, locale);
        byte[] emailHash = cipher.lookupHash(normalized);
        PendingRegistration previous = store.pendingByEmailHashForUpdate(emailHash).orElse(null);
        if (store.accountByEmailHash(emailHash).isPresent()) throw new ConflictException("Email đã được đăng ký.");
        UUID id = previous == null ? ids.next() : previous.id();
        String token = randomToken();
        String code = verificationCode();
        var encrypted = cipher.encrypt("pending_registrations.email", id, id, normalized);
        try {
            store.savePendingRegistration(new PendingRegistration(id, emailHash, encrypted, passwords.encode(password),
                    timezone, locale, termsVersion, privacyVersion, cipher.tokenHash(token),
                    cipher.tokenHash("VERIFY_EMAIL_CODE:" + id + ":" + code),
                    now.plus(24, ChronoUnit.HOURS), now.plus(10, ChronoUnit.MINUTES), 0), now);
        } catch (DataIntegrityViolationException e) { throw new ConflictException("Email đã được đăng ký."); }
        delivery.send(normalized, token, code);
        return id;
    }

    @Transactional
    public void requestVerification(String email, String remoteAddress) {
        Instant now = clock.instant();
        rate("verify:" + remoteAddress, now, 10);
        String normalized = normalizeEmail(email);
        store.pendingByEmailHashForUpdate(cipher.lookupHash(normalized))
                .ifPresent(pending -> sendVerification(pending, normalized, now));
    }

    @Transactional
    public void confirmVerification(String token) {
        Instant now = clock.instant();
        byte[] hash = cipher.tokenHash(token);
        PendingRegistration pending = store.pendingByTokenHashForUpdate(hash).orElse(null);
        if (pending == null) {
            if (store.actionTokenAlreadyConsumed(hash, "VERIFY_EMAIL")) return;
            throw new InvalidCredentialsException();
        }
        if (!pending.expiresAt().isAfter(now)) throw new InvalidCredentialsException();
        completeRegistration(pending, now);
    }

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public void confirmVerificationCode(String email, String code, String remoteAddress) {
        Instant now = clock.instant();
        String normalized = normalizeEmail(email);
        rate("verify-code-ip:" + remoteAddress, now, 30);
        rate("verify-code-email:" + normalized, now, 10);
        PendingRegistration pending = store.pendingByEmailHashForUpdate(cipher.lookupHash(normalized))
                .orElseThrow(InvalidCredentialsException::new);
        if (!pending.codeExpiresAt().isAfter(now) || pending.codeFailedAttempts() >= 5)
            throw new InvalidCredentialsException();
        byte[] codeHash = cipher.tokenHash("VERIFY_EMAIL_CODE:" + pending.id() + ":" + code);
        if (!MessageDigest.isEqual(pending.codeHash(), codeHash)) {
            store.recordPendingCodeFailure(pending.id());
            throw new InvalidCredentialsException();
        }
        completeRegistration(pending, now);
    }

    public AuthResult loginWithGoogle(GoogleAuthRequest request, String remoteAddress) {
        if (!properties.identity().oauth().google().enabled()) {
            throw new ResourceNotFoundException("Google OAuth chưa được bật.");
        }

        Instant now = clock.instant();
        rate("oauth-google-ip:" + remoteAddress, now, 30);

        GoogleOAuthClient.GoogleTokenPayload payload = googleClient.exchangeCode(request.code());
        rate("oauth-google-sub:" + payload.sub(), now, 10);

        String refresh = randomToken();
        try {
            return provisioningService.completeGoogleAuthTransaction(payload, request, refresh, now);
        } catch (DataIntegrityViolationException e) {
            return provisioningService.completeGoogleAuthTransaction(payload, request, refresh, now);
        }
    }

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public AuthResult login(String email, String password, String deviceName, String remoteAddress) {
        Instant now = clock.instant();
        String normalized = normalizeEmail(email);
        rate("login-ip:" + remoteAddress, now, 30);
        rate("login-email:" + normalized, now, 10);
        Account account = store.accountByEmailHashForUpdate(cipher.lookupHash(normalized)).orElse(null);
        if (account == null) { passwords.matches(password, DUMMY_HASH); throw new InvalidCredentialsException(); }
        if (account.lockedUntil() != null && account.lockedUntil().isAfter(now)) throw new InvalidCredentialsException();
        if (account.passwordHash() == null) { passwords.matches(password, DUMMY_HASH); throw new InvalidCredentialsException(); }
        if (!passwords.matches(password, account.passwordHash())) {
            int failures = account.failedLogins() + 1;
            store.setLoginFailure(account.id(), failures, failures >= 5 ? now.plus(15, ChronoUnit.MINUTES) : null, now);
            throw new InvalidCredentialsException();
        }
        if (!"ACTIVE".equals(account.status())) throw new InvalidCredentialsException();
        store.setLoginSuccess(account.id(), now);
        UUID sessionId = ids.next();
        String refresh = randomToken();
        store.createSession(new Session(sessionId, account.id(), ids.next(), cipher.tokenHash(refresh),
                now.plus(30, ChronoUnit.DAYS), null, now, now, deviceName));
        return new AuthResult(account.id(), accessTokens.issue(account.id(), sessionId), refresh, 600);
    }

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public AuthResult refresh(String oldToken) {
        Instant now = clock.instant();
        byte[] hash = cipher.tokenHash(oldToken);
        Session session = store.sessionForCurrentToken(hash).orElse(null);
        if (session == null) {
            store.sessionForUsedToken(hash).ifPresent(used -> {
                store.revokeFamily(used.familyId(), "TOKEN_REUSE", now);
                store.audit(used.userId(), "REFRESH_REUSE_REVOKED", used.id(), now);
            });
            throw new InvalidCredentialsException();
        }
        Account account = store.accountById(session.userId()).orElseThrow(InvalidCredentialsException::new);
        if (session.revokedAt() != null || !session.expiresAt().isAfter(now) || !"ACTIVE".equals(account.status()))
            throw new InvalidCredentialsException();
        String next = randomToken();
        store.rotate(session.id(), hash, cipher.tokenHash(next), now.plus(30, ChronoUnit.DAYS), now);
        return new AuthResult(account.id(), accessTokens.issue(account.id(), session.id()), next, 600);
    }

    @Transactional public void logout(UUID userId, UUID sessionId) {
        store.revokeSession(userId, sessionId, "LOGOUT", clock.instant());
    }

    @Transactional(readOnly = true)
    public List<SessionSummary> sessions(UUID userId) {
        return store.sessions(userId).stream().map(s -> new SessionSummary(s.id(), s.deviceName(),
                s.createdAt(), s.lastUsedAt(), s.expiresAt())).toList();
    }
    @Transactional(readOnly = true)
    public List<SessionSummary> exportSessions(UUID userId) {
        return store.allSessions(userId).stream().map(s -> new SessionSummary(s.id(),s.deviceName(),
                s.createdAt(),s.lastUsedAt(),s.expiresAt())).toList();
    }

    @Transactional public void revokeSession(UUID userId, UUID sessionId) {
        if (store.sessions(userId).stream().noneMatch(s -> s.id().equals(sessionId)))
            throw new ResourceNotFoundException("Không tìm thấy phiên đăng nhập.");
        Instant now = clock.instant();
        store.revokeSession(userId, sessionId, "USER_REVOKED", now);
        store.audit(userId, "SESSION_REVOKED", sessionId, now);
    }

    @Transactional public void revokeOtherSessions(UUID userId, UUID currentSession) {
        Instant now = clock.instant();
        store.revokeOtherSessions(userId, currentSession, now);
        store.audit(userId, "OTHER_SESSIONS_REVOKED", currentSession, now);
    }

    @Transactional(readOnly = true)
    public void reauthenticate(UUID userId, String password) {
        Account account = store.accountById(userId).orElseThrow(InvalidCredentialsException::new);
        if (!"ACTIVE".equals(account.status()) || password == null || account.passwordHash() == null
                || !passwords.matches(password, account.passwordHash())) throw new InvalidCredentialsException();
    }

    @Transactional(readOnly = true)
    public String exportEmail(UUID userId) {
        Account account = store.accountById(userId).orElseThrow(InvalidCredentialsException::new);
        return cipher.decrypt("users.email", userId, userId, account.email());
    }

    @Transactional(readOnly = true)
    public boolean isActive(UUID userId) {
        return store.accountById(userId).map(account -> "ACTIVE".equals(account.status())).orElse(false);
    }

    /** Shared, durable quota for authenticated write actions across API instances. */
    @Transactional(noRollbackFor = RateLimitExceededException.class)
    public void checkWriteQuota(UUID userId, String action, int maximum, long windowSeconds) {
        if (!store.recordRateLimit(cipher.lookupHash("write:" + action + ":" + userId),
                clock.instant(), maximum, windowSeconds)) {
            throw new RateLimitExceededException();
        }
    }

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public UUID authenticateForCancellation(String email, String password, String remoteAddress) {
        Instant now = clock.instant();
        String normalized = normalizeEmail(email);
        rate("deletion-cancel-ip:" + remoteAddress, now, 10);
        rate("deletion-cancel-email:" + normalized, now, 5);
        Account account = store.accountByEmailHash(cipher.lookupHash(normalized)).orElse(null);
        if (account == null) { passwords.matches(password, DUMMY_HASH); throw new InvalidCredentialsException(); }
        if (!"DELETION_PENDING".equals(account.status()) || password == null || account.passwordHash() == null
                || !passwords.matches(password, account.passwordHash())) throw new InvalidCredentialsException();
        return account.id();
    }

    @Transactional
    public void beginDeletion(UUID userId) {
        store.setStatus(userId, "DELETION_PENDING", clock.instant());
        store.revokeAllSessions(userId, "ACCOUNT_DELETION", clock.instant());
    }

    @Transactional public void cancelDeletion(UUID userId) {
        store.setStatus(userId, "ACTIVE", clock.instant());
    }

    private void sendVerification(PendingRegistration pending, String email, Instant now) {
        String token = randomToken();
        String code = verificationCode();
        store.updatePendingChallenge(pending.id(), cipher.tokenHash(token),
                cipher.tokenHash("VERIFY_EMAIL_CODE:" + pending.id() + ":" + code),
                now.plus(24, ChronoUnit.HOURS), now.plus(10, ChronoUnit.MINUTES), now);
        delivery.send(email, token, code);
    }

    private void completeRegistration(PendingRegistration pending, Instant now) {
        String email = cipher.decrypt("pending_registrations.email", pending.id(), pending.id(), pending.email());
        var encrypted = cipher.encrypt("users.email", pending.id(), pending.id(), email);
        try {
            store.createAccount(new Account(pending.id(), pending.emailHash(), encrypted,
                    pending.passwordHash(), "ACTIVE", 0, null, 0, now), now);
        } catch (DataIntegrityViolationException e) { throw new ConflictException("Email đã được đăng ký."); }
        store.assignUserRole(pending.id(), now);
        profiles.createDefault(pending.id(), pending.timezone(), pending.locale(),
                pending.termsVersion(), pending.privacyVersion(), now);
        store.createActionToken(ids.next(), pending.id(), pending.tokenHash(), null, "VERIFY_EMAIL",
                now.plus(24, ChronoUnit.HOURS), null, now);
        store.consumeActionToken(pending.tokenHash(), "VERIFY_EMAIL", now);
        store.deletePendingRegistration(pending.id());
    }

    private String verificationCode() {
        return String.format(Locale.ROOT, "%08d", random.nextInt(100_000_000));
    }

    private void rate(String subject, Instant now, int maximum) {
        if (!store.recordRateLimit(cipher.lookupHash(subject), now, maximum, 900)) throw new RateLimitExceededException();
    }

    @Transactional(readOnly = true)
    public boolean hasPassword(UUID userId) {
        return store.hasPassword(userId);
    }

    @Transactional
    public void changePassword(UUID userId, UUID currentSessionId, ChangePasswordRequest request, String remoteAddress) {
        Instant now = clock.instant();
        byte[] rateSubject = cipher.lookupHash("change-password-attempt:" + userId);

        // 1. Kiểm tra Rate Limiting (5 lần thử trong 15 phút = 900s)
        if (store.isRateLimited(rateSubject, now, 5, 900)) {
            throw new RateLimitExceededException();
        }

        // 2. Lấy Account và kiểm tra trạng thái
        Account account = store.accountById(userId).orElseThrow(UnauthenticatedUserException::new);
        if (account.passwordHash() == null) {
            throw new PasswordChangeException("OAUTH_ONLY_ACCOUNT");
        }
        if (!"ACTIVE".equals(account.status())) {
            throw new PasswordChangeException("ACCOUNT_NOT_ACTIVE");
        }

        // 3. Xác thực mật khẩu cũ
        if (!passwords.matches(request.currentPassword(), account.passwordHash())) {
            if (rateLimitService != null) {
                rateLimitService.recordFailedAttempt(rateSubject, now, 5, 900);
            }
            throw new PasswordChangeException("CURRENT_PASSWORD_INCORRECT");
        }

        // 4. Kiểm tra mật khẩu mới (Chỉ chạy khi mật khẩu cũ đã được xác thực thành công)
        if (request.currentPassword().equals(request.newPassword())) {
            throw new PasswordChangeException("NEW_PASSWORD_SAME_AS_OLD");
        }

        byte[] newPasswordBytes = request.newPassword().getBytes(StandardCharsets.UTF_8);
        if (newPasswordBytes.length < 12 || newPasswordBytes.length > 72) {
            throw new PasswordChangeException("PASSWORD_LENGTH_INVALID");
        }

        String email = exportEmail(userId);
        String emailLocalPart = email.split("@")[0].toLowerCase();
        if (emailLocalPart.length() >= 4 && request.newPassword().toLowerCase().contains(emailLocalPart)) {
            throw new PasswordChangeException("PASSWORD_CONTAINS_USER_INFO");
        }

        // 5. Cập nhật mật khẩu mới
        String newHash = passwords.encode(request.newPassword());
        store.changePassword(userId, newHash, now);

        // 6. Xử lý thu hồi phiên (revoke)
        if (currentSessionId != null && store.sessionBelongsToUser(currentSessionId, userId)) {
            store.revokeOtherSessions(userId, currentSessionId, now);
        } else {
            log.warn("User {} changed password without valid current session id. Revoking all sessions.", userId);
            store.revokeAllSessions(userId, "PASSWORD_CHANGED", now);
        }

        // 7. Reset counter Rate Limit & Ghi Audit Log
        store.clearRateLimit(rateSubject);
        store.audit(userId, "PASSWORD_CHANGED", userId, now);

        // 8. Phát event gửi email thông báo bảo mật
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new PasswordChangedEvent(userId, email, now, remoteAddress));
        }
    }

    @Transactional
    public void requestPasswordReset(String email, String remoteAddress) {
        Instant now = clock.instant();
        String normalized = normalizeEmail(email);

        // Rate limiting
        rate("reset-req-ip:" + remoteAddress, now, 10);
        byte[] emailLookupHash = cipher.lookupHash(normalized);
        if (!store.recordRateLimit(cipher.lookupHash("reset-req-email:" + normalized), now, 5, 900)) {
            throw new RateLimitExceededException();
        }
        if (!store.recordRateLimit(cipher.lookupHash("reset-req-email-daily:" + normalized), now, 10, 86400)) {
            throw new RateLimitExceededException();
        }

        Account account = store.accountByEmailHash(emailLookupHash).orElse(null);

        if (account == null || "SUSPENDED".equals(account.status()) || "DELETED".equals(account.status())) {
            // Anti-enumeration dummy calculation
            passwords.matches("dummy", DUMMY_HASH);
            return;
        }

        // If user account is Google OAuth-only (no password hash)
        if (account.passwordHash() == null) {
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new OAuthPasswordResetAttemptEvent(account.id(), normalized, now));
            }
            return;
        }

        // Invalidate any existing unconsumed reset tokens for this user (Single Active Token)
        store.revokeExistingActionTokens(account.id(), "RESET_PASSWORD", now);

        // Generate token and OTP code
        String token = randomToken();
        String code = verificationCode();
        byte[] tokenHash = cipher.tokenHash(token);
        byte[] codeHash = cipher.tokenHash("RESET_PASSWORD_CODE:" + account.id() + ":" + code);

        // Token: 1 hour, Code: 15 minutes
        store.createActionToken(ids.next(), account.id(), tokenHash, codeHash, "RESET_PASSWORD",
                now.plus(1, ChronoUnit.HOURS), now.plus(15, ChronoUnit.MINUTES), now);

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new PasswordResetRequestedEvent(account.id(), normalized, token, code, now));
        }
    }

    @Transactional
    public void resetPasswordWithToken(String token, String newPassword, String remoteAddress) {
        Instant now = clock.instant();
        rate("reset-confirm-ip:" + remoteAddress, now, 15);

        // 1. Validate new password format & byte length (12-72 bytes UTF-8)
        validateNewPasswordPolicy(newPassword);

        // 2. Consume action token atomically
        byte[] tokenHash = cipher.tokenHash(token);
        UUID userId = store.consumeActionToken(tokenHash, "RESET_PASSWORD", now)
                .orElseThrow(InvalidCredentialsException::new);

        // 3. Validate user account & business rules
        Account account = store.accountById(userId).orElseThrow(InvalidCredentialsException::new);
        if ("SUSPENDED".equals(account.status()) || "DELETED".equals(account.status())) {
            throw new InvalidCredentialsException();
        }

        if (account.passwordHash() != null && passwords.matches(newPassword, account.passwordHash())) {
            throw new PasswordChangeException("NEW_PASSWORD_SAME_AS_OLD");
        }

        String email = exportEmail(userId);
        validatePasswordNotContainsUserPart(newPassword, email);

        // 4. Update password
        store.changePassword(userId, passwords.encode(newPassword), now);

        // 5. If email was unverified, automatically verify it
        if (account.emailVerifiedAt() == null) {
            store.setEmailVerified(userId, now);
        }

        // 6. If user was temporarily locked out due to failed logins, reset lockout
        if (account.failedLogins() > 0 || account.lockedUntil() != null) {
            store.setLoginSuccess(userId, now);
        }

        // 7. Revoke ALL active sessions (invalidates all JWTs immediately)
        store.revokeAllSessions(userId, "PASSWORD_RESET", now);

        // 8. Audit log & Security notification event
        store.audit(userId, "PASSWORD_RESET", userId, now);
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new PasswordChangedEvent(userId, email, now, remoteAddress));
        }
    }

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public void resetPasswordWithCode(String email, String code, String newPassword, String remoteAddress) {
        Instant now = clock.instant();
        String normalized = normalizeEmail(email);

        // Rate limiting
        rate("reset-confirm-code-ip:" + remoteAddress, now, 15);
        rate("reset-confirm-code-email:" + normalized, now, 5);

        // 1. Validate new password format & byte length
        validateNewPasswordPolicy(newPassword);

        // 2. Tra cứu user theo email
        byte[] emailLookupHash = cipher.lookupHash(normalized);
        Account account = store.accountByEmailHash(emailLookupHash).orElse(null);

        if (account == null || account.passwordHash() == null || "SUSPENDED".equals(account.status()) || "DELETED".equals(account.status())) {
            // Constant-time dummy check to prevent timing attack
            byte[] dummyCodeHash = cipher.tokenHash("RESET_PASSWORD_CODE:" + DUMMY_UUID + ":" + code);
            MessageDigest.isEqual(DUMMY_CODE_HASH, dummyCodeHash);
            throw new InvalidCredentialsException();
        }

        // 3. Consume OTP code atomically
        byte[] codeHash = cipher.tokenHash("RESET_PASSWORD_CODE:" + account.id() + ":" + code);
        UUID userId = store.consumeActionCode(account.id(), codeHash, "RESET_PASSWORD", now)
                .orElseThrow(InvalidCredentialsException::new);

        // 4. Validate password business rules
        if (passwords.matches(newPassword, account.passwordHash())) {
            throw new PasswordChangeException("NEW_PASSWORD_SAME_AS_OLD");
        }

        validatePasswordNotContainsUserPart(newPassword, normalized);

        // 5. Update password
        store.changePassword(userId, passwords.encode(newPassword), now);

        // 6. Auto-verify email if not verified
        if (account.emailVerifiedAt() == null) {
            store.setEmailVerified(userId, now);
        }

        // 7. Reset failed login lockout if present
        if (account.failedLogins() > 0 || account.lockedUntil() != null) {
            store.setLoginSuccess(userId, now);
        }

        // 8. Revoke all active sessions
        store.revokeAllSessions(userId, "PASSWORD_RESET", now);

        // 9. Audit log & Security notification event
        store.audit(userId, "PASSWORD_RESET", userId, now);
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new PasswordChangedEvent(userId, normalized, now, remoteAddress));
        }
    }

    private void validateNewPasswordPolicy(String newPassword) {
        if (newPassword == null) throw new PasswordChangeException("PASSWORD_LENGTH_INVALID");
        byte[] bytes = newPassword.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 12 || bytes.length > 72) {
            throw new PasswordChangeException("PASSWORD_LENGTH_INVALID");
        }
    }

    private void validatePasswordNotContainsUserPart(String newPassword, String email) {
        String emailLocalPart = email.split("@")[0].toLowerCase(Locale.ROOT);
        if (emailLocalPart.length() >= 4 && newPassword.toLowerCase(Locale.ROOT).contains(emailLocalPart)) {
            throw new PasswordChangeException("PASSWORD_CONTAINS_USER_INFO");
        }
    }

    private String randomToken() {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static final class InvalidCredentialsException extends ApplicationException {
        public InvalidCredentialsException() { super(ErrorCode.UNAUTHORIZED, "Thông tin xác thực không hợp lệ."); }
    }
}
