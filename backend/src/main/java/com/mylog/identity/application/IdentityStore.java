package com.mylog.identity.application;

import com.mylog.platform.crypto.SensitiveDataCipher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IdentityStore {
    record Account(UUID id, byte[] emailHash, SensitiveDataCipher.Encrypted email, String passwordHash,
                   String status, int failedLogins, Instant lockedUntil, long version) {}
    record Session(UUID id, UUID userId, UUID familyId, byte[] tokenHash, Instant expiresAt,
                   Instant revokedAt, Instant createdAt, Instant lastUsedAt, String deviceName) {}

    void createAccount(Account account, Instant now);
    Optional<Account> accountByEmailHash(byte[] hash);
    Optional<Account> accountByEmailHashForUpdate(byte[] hash);
    Optional<Account> accountById(UUID id);
    void setLoginFailure(UUID userId, int failures, Instant lockedUntil, Instant now);
    void setLoginSuccess(UUID userId, Instant now);
    void activate(UUID userId, Instant now);
    void changePassword(UUID userId, String hash, Instant now);
    void assignUserRole(UUID userId, Instant now);
    List<String> roles(UUID userId);
    List<String> permissions(UUID userId);
    void createSession(Session session);
    Optional<Session> sessionForCurrentToken(byte[] tokenHash);
    Optional<Session> sessionForUsedToken(byte[] tokenHash);
    Optional<Session> sessionById(UUID sessionId);
    List<Session> sessions(UUID userId);
    void rotate(UUID sessionId, byte[] oldHash, byte[] newHash, Instant expiresAt, Instant now);
    void revokeFamily(UUID familyId, String reason, Instant now);
    void revokeSession(UUID userId, UUID sessionId, String reason, Instant now);
    void revokeOtherSessions(UUID userId, UUID except, Instant now);
    void revokeAllSessions(UUID userId, String reason, Instant now);
    void createActionToken(UUID id, UUID userId, byte[] hash, String purpose, Instant expiresAt, Instant now);
    Optional<UUID> consumeActionToken(byte[] hash, String purpose, Instant now);
    boolean actionTokenAlreadyConsumed(byte[] hash, String purpose);
    boolean recordRateLimit(byte[] subjectHash, Instant now, int maxAttempts, long windowSeconds);
    void audit(UUID actorId, String action, UUID targetId, Instant now);
    void purgeExpired(Instant now);
}
