package com.mylog.identity.application;

import com.mylog.identity.application.IdentityStore.Account;
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
import com.mylog.platform.web.InvalidRequestException;
import com.mylog.user.application.UserProfileUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.IDN;
import java.nio.charset.StandardCharsets;
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
    private final IdentityStore store;
    private final SensitiveDataCipher cipher;
    private final AccessTokens accessTokens;
    private final VerificationDelivery delivery;
    private final UserProfileUseCase profiles;
    private final IdGenerator ids;
    private final Clock clock;
    private final SecureRandom random;
    private final PasswordEncoder passwords = new BCryptPasswordEncoder(12);

    public IdentityService(IdentityStore store, SensitiveDataCipher cipher, AccessTokens accessTokens,
            VerificationDelivery delivery, UserProfileUseCase profiles, IdGenerator ids, Clock clock, SecureRandom random) {
        this.store = store; this.cipher = cipher; this.accessTokens = accessTokens; this.delivery = delivery;
        this.profiles = profiles; this.ids = ids; this.clock = clock; this.random = random;
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
        byte[] emailHash = cipher.lookupHash(normalized);
        if (store.accountByEmailHash(emailHash).isPresent()) throw new ConflictException("Email đã được đăng ký.");
        UUID id = ids.next();
        var encrypted = cipher.encrypt("users.email", id, id, normalized);
        try {
            store.createAccount(new Account(id, emailHash, encrypted, passwords.encode(password), "PENDING", 0, null, 0), now);
        } catch (DataIntegrityViolationException e) { throw new ConflictException("Email đã được đăng ký."); }
        store.assignUserRole(id, now);
        profiles.createDefault(id, timezone, locale, termsVersion, privacyVersion, now);
        sendVerification(id, normalized, now);
        return id;
    }

    @Transactional
    public void requestVerification(String email, String remoteAddress) {
        Instant now = clock.instant();
        rate("verify:" + remoteAddress, now, 10);
        String normalized = normalizeEmail(email);
        store.accountByEmailHash(cipher.lookupHash(normalized)).filter(a -> a.status().equals("PENDING"))
                .ifPresent(a -> sendVerification(a.id(), normalized, now));
    }

    @Transactional
    public void confirmVerification(String token) {
        byte[] hash = cipher.tokenHash(token);
        UUID user = store.consumeActionToken(hash, "VERIFY_EMAIL", clock.instant()).orElse(null);
        if (user == null) {
            if (store.actionTokenAlreadyConsumed(hash, "VERIFY_EMAIL")) return;
            throw new InvalidCredentialsException();
        }
        store.invalidateVerificationTokens(user);
        store.activate(user, clock.instant());
    }

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public void confirmVerificationCode(String email, String code, String remoteAddress) {
        Instant now = clock.instant();
        String normalized = normalizeEmail(email);
        rate("verify-code-ip:" + remoteAddress, now, 30);
        rate("verify-code-email:" + normalized, now, 10);
        UUID user = store.accountByEmailHash(cipher.lookupHash(normalized))
                .filter(account -> "PENDING".equals(account.status()))
                .map(Account::id).orElseThrow(InvalidCredentialsException::new);
        byte[] codeHash = cipher.tokenHash("VERIFY_EMAIL_CODE:" + user + ":" + code);
        if (store.consumeActionCode(user, codeHash, now).isEmpty()) throw new InvalidCredentialsException();
        store.invalidateVerificationTokens(user);
        store.activate(user, now);
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
        if (!"ACTIVE".equals(account.status()) || password == null
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
        if (!"DELETION_PENDING".equals(account.status()) || password == null
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

    private void sendVerification(UUID id, String email, Instant now) {
        store.invalidateVerificationTokens(id);
        String token = randomToken();
        String code = String.format(Locale.ROOT, "%08d", random.nextInt(100_000_000));
        byte[] codeHash = cipher.tokenHash("VERIFY_EMAIL_CODE:" + id + ":" + code);
        store.createActionToken(ids.next(), id, cipher.tokenHash(token), codeHash, "VERIFY_EMAIL",
                now.plus(24, ChronoUnit.HOURS), now.plus(10, ChronoUnit.MINUTES), now);
        delivery.send(email, token, code);
    }

    private void rate(String subject, Instant now, int maximum) {
        if (!store.recordRateLimit(cipher.lookupHash(subject), now, maximum, 900)) throw new RateLimitExceededException();
    }

    private String randomToken() {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static final class InvalidCredentialsException extends ApplicationException {
        public InvalidCredentialsException() { super(ErrorCode.UNAUTHORIZED, "Thông tin xác thực không hợp lệ."); }
    }
}
