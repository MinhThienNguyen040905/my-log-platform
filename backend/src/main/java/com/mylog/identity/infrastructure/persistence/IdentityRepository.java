package com.mylog.identity.infrastructure.persistence;

import com.mylog.identity.application.IdentityStore;
import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.platform.id.IdGenerator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class IdentityRepository implements IdentityStore {
    private final EntityManager entityManager;
    private final IdGenerator ids;
    private final Clock clock;

    IdentityRepository(EntityManager entityManager, IdGenerator ids, Clock clock) {
        this.entityManager = entityManager;
        this.ids = ids;
        this.clock = clock;
    }

    @Override public void createAccount(Account account, Instant now) {
        User user = new User();
        user.id = account.id();
        user.emailLookupHash = account.emailHash();
        user.encryptedEmail = account.email().ciphertext();
        user.emailIv = account.email().iv();
        user.emailWrappedKey = account.email().wrappedKey();
        user.emailKeyVersion = account.email().keyVersion();
        user.passwordHash = account.passwordHash();
        user.authProvider = "LOCAL";
        user.status = account.status();
        user.createdAt = now;
        user.updatedAt = now;
        entityManager.persist(user);
        entityManager.flush();
    }

    @Override public Optional<Account> accountByEmailHash(byte[] hash) {
        return entityManager.createQuery("""
                select u from User u where u.emailLookupHash=:hash and u.deletedAt is null
                """, User.class).setParameter("hash", hash).getResultStream().findFirst().map(this::account);
    }

    @Override public Optional<Account> accountByEmailHashForUpdate(byte[] hash) {
        return entityManager.createQuery("""
                select u from User u where u.emailLookupHash=:hash and u.deletedAt is null
                """, User.class).setParameter("hash", hash).setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream().findFirst().map(this::account);
    }

    @Override public Optional<Account> accountById(UUID id) {
        User user = entityManager.find(User.class, id);
        return user == null || user.deletedAt != null ? Optional.empty() : Optional.of(account(user));
    }

    @Override public void setLoginFailure(UUID id, int failures, Instant locked, Instant now) {
        User user = entityManager.find(User.class, id);
        user.failedLoginCount = failures;
        user.lockedUntil = locked;
        user.updatedAt = now;
        user.rowVersion++;
    }

    @Override public void setLoginSuccess(UUID id, Instant now) {
        User user = entityManager.find(User.class, id);
        user.failedLoginCount = 0;
        user.lockedUntil = null;
        user.lastLoginAt = now;
        user.updatedAt = now;
        user.rowVersion++;
    }

    @Override public void activate(UUID id, Instant now) {
        User user = entityManager.find(User.class, id);
        if (user != null && "PENDING".equals(user.status)) {
            user.status = "ACTIVE";
            user.emailVerifiedAt = now;
            user.updatedAt = now;
            user.rowVersion++;
        }
    }

    @Override public void setStatus(UUID id, String status, Instant now) {
        User user = entityManager.find(User.class, id);
        if (user == null) throw new IllegalStateException("Account missing");
        user.status = status;
        user.updatedAt = now;
        user.rowVersion++;
    }

    @Override public void changePassword(UUID id, String hash, Instant now) {
        User user = entityManager.find(User.class, id);
        user.passwordHash = hash;
        user.updatedAt = now;
        user.rowVersion++;
    }

    @Override public void assignUserRole(UUID id, Instant now) {
        Role role = entityManager.createQuery("select r from Role r where r.code='USER'", Role.class)
                .getSingleResult();
        UserRole assignment = new UserRole();
        assignment.userId = id;
        assignment.roleId = role.id;
        assignment.assignedAt = now;
        entityManager.persist(assignment);
    }

    @Override public List<String> roles(UUID id) {
        return entityManager.createQuery("""
                select r.code from UserRole ur, Role r
                where ur.roleId=r.id and ur.userId=:userId
                  and (ur.expiresAt is null or ur.expiresAt>:now)
                """, String.class).setParameter("userId", id).setParameter("now", clock.instant()).getResultList();
    }

    @Override public List<String> permissions(UUID id) {
        return entityManager.createQuery("""
                select p.code from UserRole ur, RolePermission rp, Permission p
                where ur.roleId=rp.roleId and rp.permissionId=p.id and ur.userId=:userId
                  and (ur.expiresAt is null or ur.expiresAt>:now)
                """, String.class).setParameter("userId", id).setParameter("now", clock.instant()).getResultList();
    }

    @Override public void createSession(Session session) {
        AuthSession authSession = new AuthSession();
        authSession.id = session.id();
        authSession.userId = session.userId();
        authSession.tokenFamilyId = session.familyId();
        authSession.currentTokenHash = session.tokenHash();
        authSession.deviceName = session.deviceName();
        authSession.lastUsedAt = session.lastUsedAt();
        authSession.expiresAt = session.expiresAt();
        authSession.createdAt = session.createdAt();
        entityManager.persist(authSession);
    }

    @Override public Optional<Session> sessionForCurrentToken(byte[] hash) {
        return entityManager.createQuery("select s from AuthSession s where s.currentTokenHash=:hash", AuthSession.class)
                .setParameter("hash", hash).setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream().findFirst().map(this::session);
    }

    @Override public Optional<Session> sessionForUsedToken(byte[] hash) {
        return entityManager.createQuery("""
                select s from AuthRefreshHistory h, AuthSession s
                where h.sessionId=s.id and h.tokenHash=:hash
                """, AuthSession.class).setParameter("hash", hash)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultStream().findFirst().map(this::session);
    }

    @Override public Optional<Session> sessionById(UUID id) {
        return Optional.ofNullable(entityManager.find(AuthSession.class, id)).map(this::session);
    }

    @Override public List<Session> sessions(UUID userId) {
        return entityManager.createQuery("""
                select s from AuthSession s where s.userId=:userId
                  and s.revokedAt is null and s.expiresAt>:now order by s.createdAt desc
                """, AuthSession.class).setParameter("userId", userId).setParameter("now", clock.instant())
                .getResultStream().map(this::session).toList();
    }
    @Override public List<Session> allSessions(UUID userId) {
        return entityManager.createQuery("select s from AuthSession s where s.userId=:user order by s.createdAt desc",AuthSession.class)
                .setParameter("user",userId).getResultList().stream().map(this::session).toList();
    }

    @Override public void rotate(UUID sessionId, byte[] oldHash, byte[] newHash, Instant expiry, Instant now) {
        AuthRefreshHistory history = new AuthRefreshHistory();
        history.id = ids.next();
        history.tokenHash = oldHash;
        history.sessionId = sessionId;
        history.usedAt = now;
        entityManager.persist(history);
        AuthSession session = entityManager.find(AuthSession.class, sessionId);
        session.currentTokenHash = newHash;
        session.expiresAt = expiry;
        session.lastUsedAt = now;
    }

    @Override public void revokeFamily(UUID family, String reason, Instant now) {
        entityManager.createQuery("""
                update AuthSession s set s.revokedAt=:now, s.revokeReason=:reason
                where s.tokenFamilyId=:family and s.revokedAt is null
                """).setParameter("now", now).setParameter("reason", reason)
                .setParameter("family", family).executeUpdate();
        entityManager.clear();
    }

    @Override public void revokeSession(UUID userId, UUID sessionId, String reason, Instant now) {
        entityManager.createQuery("""
                update AuthSession s set s.revokedAt=:now, s.revokeReason=:reason
                where s.id=:sessionId and s.userId=:userId and s.revokedAt is null
                """).setParameter("now", now).setParameter("reason", reason)
                .setParameter("sessionId", sessionId).setParameter("userId", userId).executeUpdate();
        entityManager.clear();
    }

    @Override public void revokeOtherSessions(UUID userId, UUID except, Instant now) {
        entityManager.createQuery("""
                update AuthSession s set s.revokedAt=:now, s.revokeReason='USER_REVOKED'
                where s.userId=:userId and s.id<>:except and s.revokedAt is null
                """).setParameter("now", now).setParameter("userId", userId)
                .setParameter("except", except).executeUpdate();
        entityManager.clear();
    }

    @Override public void revokeAllSessions(UUID userId, String reason, Instant now) {
        entityManager.flush();
        entityManager.createQuery("""
                update AuthSession s set s.revokedAt=:now, s.revokeReason=:reason
                where s.userId=:userId and s.revokedAt is null
                """).setParameter("now", now).setParameter("reason", reason)
                .setParameter("userId", userId).executeUpdate();
        entityManager.clear();
    }

    @Override public void createActionToken(UUID id, UUID userId, byte[] hash, String purpose, Instant expiresAt, Instant now) {
        AuthActionToken token = new AuthActionToken();
        token.id = id;
        token.userId = userId;
        token.tokenHash = hash;
        token.purpose = purpose;
        token.expiresAt = expiresAt;
        token.createdAt = now;
        entityManager.persist(token);
    }

    @Override public Optional<UUID> consumeActionToken(byte[] hash, String purpose, Instant now) {
        return entityManager.createQuery("""
                select t from AuthActionToken t where t.tokenHash=:hash and t.purpose=:purpose
                """, AuthActionToken.class).setParameter("hash", hash).setParameter("purpose", purpose)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultStream().findFirst()
                .filter(t -> t.consumedAt == null && t.expiresAt.isAfter(now))
                .map(t -> { t.consumedAt = now; return t.userId; });
    }

    @Override public boolean actionTokenAlreadyConsumed(byte[] hash, String purpose) {
        return entityManager.createQuery("""
                select count(t) from AuthActionToken t where t.tokenHash=:hash
                  and t.purpose=:purpose and t.consumedAt is not null
                """, Long.class).setParameter("hash", hash).setParameter("purpose", purpose).getSingleResult() > 0;
    }

    @Override public boolean recordRateLimit(byte[] subject, Instant now, int maximum, long windowSeconds) {
        Instant cutoff = now.minusSeconds(windowSeconds);
        entityManager.createNativeQuery("""
                INSERT INTO auth_rate_limits(subject_hash,attempts,window_started_at,blocked_until) VALUES (?1,1,?2,NULL)
                ON CONFLICT (subject_hash) DO UPDATE SET
                  attempts=CASE WHEN auth_rate_limits.window_started_at<?3 THEN 1 ELSE auth_rate_limits.attempts+1 END,
                  window_started_at=CASE WHEN auth_rate_limits.window_started_at<?3 THEN ?2 ELSE auth_rate_limits.window_started_at END
                """).setParameter(1, subject).setParameter(2, Timestamp.from(now))
                .setParameter(3, Timestamp.from(cutoff)).executeUpdate();
        Number attempts = (Number) entityManager.createNativeQuery(
                "SELECT attempts FROM auth_rate_limits WHERE subject_hash=?1")
                .setParameter(1, subject).getSingleResult();
        return attempts.intValue() <= maximum;
    }

    @Override public void audit(UUID actor, String action, UUID target, Instant now) {
        AuditLog auditLog = new AuditLog();
        auditLog.id = ids.next();
        auditLog.actorUserId = actor;
        auditLog.actorType = "USER";
        auditLog.action = action;
        auditLog.targetType = "AUTH_SESSION";
        auditLog.targetId = target;
        auditLog.occurredAt = now;
        entityManager.persist(auditLog);
    }

    @Override @Transactional public void purgeExpired(Instant now) {
        entityManager.createQuery("delete from AuthActionToken t where t.expiresAt<:cutoff or t.consumedAt<:consumed")
                .setParameter("cutoff", now.minusSeconds(7 * 86400L))
                .setParameter("consumed", now.minusSeconds(86400)).executeUpdate();
        entityManager.createQuery("delete from AuthSession s where s.expiresAt<:cutoff")
                .setParameter("cutoff", now.minusSeconds(7 * 86400L)).executeUpdate();
        entityManager.createQuery("delete from AuthRateLimit r where r.windowStartedAt<:cutoff")
                .setParameter("cutoff", now.minusSeconds(86400)).executeUpdate();
    }

    private Account account(User user) {
        return new Account(user.id, user.emailLookupHash,
                new SensitiveDataCipher.Encrypted(user.encryptedEmail, user.emailIv,
                        user.emailWrappedKey, user.emailKeyVersion), user.passwordHash,
                user.status, user.failedLoginCount, user.lockedUntil, user.rowVersion);
    }

    private Session session(AuthSession authSession) {
        return new Session(authSession.id, authSession.userId, authSession.tokenFamilyId, authSession.currentTokenHash,
                authSession.expiresAt, authSession.revokedAt, authSession.createdAt, authSession.lastUsedAt, authSession.deviceName);
    }
}
