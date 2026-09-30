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
        UserEntity entity = new UserEntity();
        entity.id = account.id();
        entity.emailLookupHash = account.emailHash();
        entity.encryptedEmail = account.email().ciphertext();
        entity.emailIv = account.email().iv();
        entity.emailWrappedKey = account.email().wrappedKey();
        entity.emailKeyVersion = account.email().keyVersion();
        entity.passwordHash = account.passwordHash();
        entity.authProvider = "LOCAL";
        entity.status = account.status();
        entity.createdAt = now;
        entity.updatedAt = now;
        entityManager.persist(entity);
        entityManager.flush();
    }

    @Override public Optional<Account> accountByEmailHash(byte[] hash) {
        return entityManager.createQuery("""
                select u from UserEntity u where u.emailLookupHash=:hash and u.deletedAt is null
                """, UserEntity.class).setParameter("hash", hash).getResultStream().findFirst().map(this::account);
    }

    @Override public Optional<Account> accountByEmailHashForUpdate(byte[] hash) {
        return entityManager.createQuery("""
                select u from UserEntity u where u.emailLookupHash=:hash and u.deletedAt is null
                """, UserEntity.class).setParameter("hash", hash).setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream().findFirst().map(this::account);
    }

    @Override public Optional<Account> accountById(UUID id) {
        UserEntity entity = entityManager.find(UserEntity.class, id);
        return entity == null || entity.deletedAt != null ? Optional.empty() : Optional.of(account(entity));
    }

    @Override public void setLoginFailure(UUID id, int failures, Instant locked, Instant now) {
        UserEntity entity = entityManager.find(UserEntity.class, id);
        entity.failedLoginCount = failures;
        entity.lockedUntil = locked;
        entity.updatedAt = now;
        entity.rowVersion++;
    }

    @Override public void setLoginSuccess(UUID id, Instant now) {
        UserEntity entity = entityManager.find(UserEntity.class, id);
        entity.failedLoginCount = 0;
        entity.lockedUntil = null;
        entity.lastLoginAt = now;
        entity.updatedAt = now;
        entity.rowVersion++;
    }

    @Override public void activate(UUID id, Instant now) {
        UserEntity entity = entityManager.find(UserEntity.class, id);
        if (entity != null && "PENDING".equals(entity.status)) {
            entity.status = "ACTIVE";
            entity.emailVerifiedAt = now;
            entity.updatedAt = now;
            entity.rowVersion++;
        }
    }

    @Override public void changePassword(UUID id, String hash, Instant now) {
        UserEntity entity = entityManager.find(UserEntity.class, id);
        entity.passwordHash = hash;
        entity.updatedAt = now;
        entity.rowVersion++;
    }

    @Override public void assignUserRole(UUID id, Instant now) {
        RoleEntity role = entityManager.createQuery("select r from RoleEntity r where r.code='USER'", RoleEntity.class)
                .getSingleResult();
        UserRoleEntity assignment = new UserRoleEntity();
        assignment.userId = id;
        assignment.roleId = role.id;
        assignment.assignedAt = now;
        entityManager.persist(assignment);
    }

    @Override public List<String> roles(UUID id) {
        return entityManager.createQuery("""
                select r.code from UserRoleEntity ur, RoleEntity r
                where ur.roleId=r.id and ur.userId=:userId
                  and (ur.expiresAt is null or ur.expiresAt>:now)
                """, String.class).setParameter("userId", id).setParameter("now", clock.instant()).getResultList();
    }

    @Override public List<String> permissions(UUID id) {
        return entityManager.createQuery("""
                select p.code from UserRoleEntity ur, RolePermissionEntity rp, PermissionEntity p
                where ur.roleId=rp.roleId and rp.permissionId=p.id and ur.userId=:userId
                  and (ur.expiresAt is null or ur.expiresAt>:now)
                """, String.class).setParameter("userId", id).setParameter("now", clock.instant()).getResultList();
    }

    @Override public void createSession(Session session) {
        AuthSessionEntity entity = new AuthSessionEntity();
        entity.id = session.id();
        entity.userId = session.userId();
        entity.tokenFamilyId = session.familyId();
        entity.currentTokenHash = session.tokenHash();
        entity.deviceName = session.deviceName();
        entity.lastUsedAt = session.lastUsedAt();
        entity.expiresAt = session.expiresAt();
        entity.createdAt = session.createdAt();
        entityManager.persist(entity);
    }

    @Override public Optional<Session> sessionForCurrentToken(byte[] hash) {
        return entityManager.createQuery("select s from AuthSessionEntity s where s.currentTokenHash=:hash", AuthSessionEntity.class)
                .setParameter("hash", hash).setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream().findFirst().map(this::session);
    }

    @Override public Optional<Session> sessionForUsedToken(byte[] hash) {
        return entityManager.createQuery("""
                select s from AuthRefreshHistoryEntity h, AuthSessionEntity s
                where h.sessionId=s.id and h.tokenHash=:hash
                """, AuthSessionEntity.class).setParameter("hash", hash)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultStream().findFirst().map(this::session);
    }

    @Override public Optional<Session> sessionById(UUID id) {
        return Optional.ofNullable(entityManager.find(AuthSessionEntity.class, id)).map(this::session);
    }

    @Override public List<Session> sessions(UUID userId) {
        return entityManager.createQuery("""
                select s from AuthSessionEntity s where s.userId=:userId
                  and s.revokedAt is null and s.expiresAt>:now order by s.createdAt desc
                """, AuthSessionEntity.class).setParameter("userId", userId).setParameter("now", clock.instant())
                .getResultStream().map(this::session).toList();
    }

    @Override public void rotate(UUID sessionId, byte[] oldHash, byte[] newHash, Instant expiry, Instant now) {
        AuthRefreshHistoryEntity history = new AuthRefreshHistoryEntity();
        history.tokenHash = oldHash;
        history.sessionId = sessionId;
        history.usedAt = now;
        entityManager.persist(history);
        AuthSessionEntity session = entityManager.find(AuthSessionEntity.class, sessionId);
        session.currentTokenHash = newHash;
        session.expiresAt = expiry;
        session.lastUsedAt = now;
    }

    @Override public void revokeFamily(UUID family, String reason, Instant now) {
        entityManager.createQuery("""
                update AuthSessionEntity s set s.revokedAt=:now, s.revokeReason=:reason
                where s.tokenFamilyId=:family and s.revokedAt is null
                """).setParameter("now", now).setParameter("reason", reason)
                .setParameter("family", family).executeUpdate();
        entityManager.clear();
    }

    @Override public void revokeSession(UUID userId, UUID sessionId, String reason, Instant now) {
        entityManager.createQuery("""
                update AuthSessionEntity s set s.revokedAt=:now, s.revokeReason=:reason
                where s.id=:sessionId and s.userId=:userId and s.revokedAt is null
                """).setParameter("now", now).setParameter("reason", reason)
                .setParameter("sessionId", sessionId).setParameter("userId", userId).executeUpdate();
        entityManager.clear();
    }

    @Override public void revokeOtherSessions(UUID userId, UUID except, Instant now) {
        entityManager.createQuery("""
                update AuthSessionEntity s set s.revokedAt=:now, s.revokeReason='USER_REVOKED'
                where s.userId=:userId and s.id<>:except and s.revokedAt is null
                """).setParameter("now", now).setParameter("userId", userId)
                .setParameter("except", except).executeUpdate();
        entityManager.clear();
    }

    @Override public void revokeAllSessions(UUID userId, String reason, Instant now) {
        entityManager.createQuery("""
                update AuthSessionEntity s set s.revokedAt=:now, s.revokeReason=:reason
                where s.userId=:userId and s.revokedAt is null
                """).setParameter("now", now).setParameter("reason", reason)
                .setParameter("userId", userId).executeUpdate();
        entityManager.clear();
    }

    @Override public void createActionToken(UUID id, UUID userId, byte[] hash, String purpose, Instant expiresAt, Instant now) {
        AuthActionTokenEntity entity = new AuthActionTokenEntity();
        entity.id = id;
        entity.userId = userId;
        entity.tokenHash = hash;
        entity.purpose = purpose;
        entity.expiresAt = expiresAt;
        entity.createdAt = now;
        entityManager.persist(entity);
    }

    @Override public Optional<UUID> consumeActionToken(byte[] hash, String purpose, Instant now) {
        return entityManager.createQuery("""
                select t from AuthActionTokenEntity t where t.tokenHash=:hash and t.purpose=:purpose
                """, AuthActionTokenEntity.class).setParameter("hash", hash).setParameter("purpose", purpose)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultStream().findFirst()
                .filter(t -> t.consumedAt == null && t.expiresAt.isAfter(now))
                .map(t -> { t.consumedAt = now; return t.userId; });
    }

    @Override public boolean actionTokenAlreadyConsumed(byte[] hash, String purpose) {
        return entityManager.createQuery("""
                select count(t) from AuthActionTokenEntity t where t.tokenHash=:hash
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
        AuditLogEntity entity = new AuditLogEntity();
        entity.id = ids.next();
        entity.actorUserId = actor;
        entity.actorType = "USER";
        entity.action = action;
        entity.targetType = "AUTH_SESSION";
        entity.targetId = target;
        entity.occurredAt = now;
        entityManager.persist(entity);
    }

    @Override @Transactional public void purgeExpired(Instant now) {
        entityManager.createQuery("delete from AuthActionTokenEntity t where t.expiresAt<:cutoff or t.consumedAt<:consumed")
                .setParameter("cutoff", now.minusSeconds(7 * 86400L))
                .setParameter("consumed", now.minusSeconds(86400)).executeUpdate();
        entityManager.createQuery("delete from AuthSessionEntity s where s.expiresAt<:cutoff")
                .setParameter("cutoff", now.minusSeconds(7 * 86400L)).executeUpdate();
        entityManager.createQuery("delete from AuthRateLimitEntity r where r.windowStartedAt<:cutoff")
                .setParameter("cutoff", now.minusSeconds(86400)).executeUpdate();
    }

    private Account account(UserEntity entity) {
        return new Account(entity.id, entity.emailLookupHash,
                new SensitiveDataCipher.Encrypted(entity.encryptedEmail, entity.emailIv,
                        entity.emailWrappedKey, entity.emailKeyVersion), entity.passwordHash,
                entity.status, entity.failedLoginCount, entity.lockedUntil, entity.rowVersion);
    }

    private Session session(AuthSessionEntity entity) {
        return new Session(entity.id, entity.userId, entity.tokenFamilyId, entity.currentTokenHash,
                entity.expiresAt, entity.revokedAt, entity.createdAt, entity.lastUsedAt, entity.deviceName);
    }
}
