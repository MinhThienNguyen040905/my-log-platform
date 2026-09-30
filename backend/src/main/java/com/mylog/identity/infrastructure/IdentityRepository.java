package com.mylog.identity.infrastructure;

import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.identity.application.IdentityStore;
import com.mylog.platform.id.IdGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class IdentityRepository implements IdentityStore {
    private final JdbcTemplate jdbc;
    private final IdGenerator ids;
    private final Clock clock;

    IdentityRepository(JdbcTemplate jdbc, IdGenerator ids, Clock clock) { this.jdbc = jdbc; this.ids = ids; this.clock = clock; }

    private static Instant instant(ResultSet rs, String name) throws SQLException {
        Timestamp value = rs.getTimestamp(name);
        return value == null ? null : value.toInstant();
    }
    private static Timestamp time(Instant value) { return value == null ? null : Timestamp.from(value); }
    private static UUID uuid(ResultSet rs, String name) throws SQLException { return (UUID) rs.getObject(name); }

    private static final RowMapper<Account> ACCOUNT = (rs, row) -> new Account(
            uuid(rs, "id"), rs.getBytes("email_lookup_hash"),
            new SensitiveDataCipher.Encrypted(rs.getBytes("encrypted_email"), rs.getBytes("email_iv"),
                    rs.getBytes("email_wrapped_key"), rs.getString("email_key_version")),
            rs.getString("password_hash"), rs.getString("status"), rs.getInt("failed_login_count"),
            instant(rs, "locked_until"), rs.getLong("row_version"));
    private static final RowMapper<Session> SESSION = (rs, row) -> new Session(
            uuid(rs, "id"), uuid(rs, "user_id"), uuid(rs, "token_family_id"),
            rs.getBytes("current_token_hash"), instant(rs, "expires_at"), instant(rs, "revoked_at"),
            instant(rs, "created_at"), instant(rs, "last_used_at"), rs.getString("device_name"));

    @Override public void createAccount(Account a, Instant now) {
        jdbc.update("""
                INSERT INTO users (id,email_lookup_hash,encrypted_email,email_iv,email_wrapped_key,email_key_version,
                 password_hash,auth_provider,status,created_at,updated_at)
                VALUES (?,?,?,?,?,?,?,'LOCAL',?,?,?)
                """, a.id(), a.emailHash(), a.email().ciphertext(), a.email().iv(), a.email().wrappedKey(),
                a.email().keyVersion(), a.passwordHash(), a.status(), time(now), time(now));
    }
    @Override public Optional<Account> accountByEmailHash(byte[] hash) {
        return jdbc.query("SELECT * FROM users WHERE email_lookup_hash=? AND deleted_at IS NULL", ACCOUNT, hash).stream().findFirst();
    }
    @Override public Optional<Account> accountByEmailHashForUpdate(byte[] hash) {
        return jdbc.query("SELECT * FROM users WHERE email_lookup_hash=? AND deleted_at IS NULL FOR UPDATE", ACCOUNT, hash).stream().findFirst();
    }
    @Override public Optional<Account> accountById(UUID id) {
        return jdbc.query("SELECT * FROM users WHERE id=? AND deleted_at IS NULL", ACCOUNT, id).stream().findFirst();
    }
    @Override public void setLoginFailure(UUID id, int failures, Instant locked, Instant now) {
        jdbc.update("UPDATE users SET failed_login_count=?,locked_until=?,updated_at=?,row_version=row_version+1 WHERE id=?",
                failures, time(locked), time(now), id);
    }
    @Override public void setLoginSuccess(UUID id, Instant now) {
        jdbc.update("UPDATE users SET failed_login_count=0,locked_until=NULL,last_login_at=?,updated_at=?,row_version=row_version+1 WHERE id=?",
                time(now), time(now), id);
    }
    @Override public void activate(UUID id, Instant now) {
        jdbc.update("UPDATE users SET status='ACTIVE',email_verified_at=?,updated_at=?,row_version=row_version+1 WHERE id=? AND status='PENDING'",
                time(now), time(now), id);
    }
    @Override public void changePassword(UUID id, String hash, Instant now) {
        jdbc.update("UPDATE users SET password_hash=?,updated_at=?,row_version=row_version+1 WHERE id=?", hash, time(now), id);
    }
    @Override public void assignUserRole(UUID id, Instant now) {
        jdbc.update("INSERT INTO user_roles(user_id,role_id,assigned_at) SELECT ?,id,? FROM roles WHERE code='USER'", id, time(now));
    }
    @Override public List<String> roles(UUID id) {
        return jdbc.queryForList("""
                SELECT r.code FROM user_roles ur JOIN roles r ON r.id=ur.role_id
                WHERE ur.user_id=? AND (ur.expires_at IS NULL OR ur.expires_at>?)
                """, String.class, id, time(clock.instant()));
    }
    @Override public List<String> permissions(UUID id) {
        return jdbc.queryForList("""
                SELECT p.code FROM user_roles ur JOIN role_permissions rp ON rp.role_id=ur.role_id
                JOIN permissions p ON p.id=rp.permission_id WHERE ur.user_id=? AND (ur.expires_at IS NULL OR ur.expires_at>?)
                """, String.class, id, time(clock.instant()));
    }
    @Override public void createSession(Session s) {
        jdbc.update("""
                INSERT INTO auth_sessions(id,user_id,token_family_id,current_token_hash,device_name,last_used_at,expires_at,created_at)
                VALUES (?,?,?,?,?,?,?,?)
                """, s.id(), s.userId(), s.familyId(), s.tokenHash(), s.deviceName(), time(s.lastUsedAt()), time(s.expiresAt()), time(s.createdAt()));
    }
    @Override public Optional<Session> sessionForCurrentToken(byte[] hash) {
        return jdbc.query("SELECT * FROM auth_sessions WHERE current_token_hash=? FOR UPDATE", SESSION, hash).stream().findFirst();
    }
    @Override public Optional<Session> sessionForUsedToken(byte[] hash) {
        return jdbc.query("""
                SELECT s.* FROM auth_refresh_history h JOIN auth_sessions s ON s.id=h.session_id
                WHERE h.token_hash=? FOR UPDATE OF s
                """, SESSION, hash).stream().findFirst();
    }
    @Override public Optional<Session> sessionById(UUID id) {
        return jdbc.query("SELECT * FROM auth_sessions WHERE id=?", SESSION, id).stream().findFirst();
    }
    @Override public List<Session> sessions(UUID id) {
        return jdbc.query("SELECT * FROM auth_sessions WHERE user_id=? AND revoked_at IS NULL AND expires_at>? ORDER BY created_at DESC",
                SESSION, id, time(clock.instant()));
    }
    @Override public void rotate(UUID sessionId, byte[] oldHash, byte[] newHash, Instant expiry, Instant now) {
        jdbc.update("INSERT INTO auth_refresh_history(token_hash,session_id,used_at) VALUES (?,?,?)", oldHash, sessionId, time(now));
        jdbc.update("UPDATE auth_sessions SET current_token_hash=?,expires_at=?,last_used_at=? WHERE id=?", newHash, time(expiry), time(now), sessionId);
    }
    @Override public void revokeFamily(UUID family, String reason, Instant now) {
        jdbc.update("UPDATE auth_sessions SET revoked_at=?,revoke_reason=? WHERE token_family_id=? AND revoked_at IS NULL", time(now), reason, family);
    }
    @Override public void revokeSession(UUID user, UUID session, String reason, Instant now) {
        jdbc.update("UPDATE auth_sessions SET revoked_at=?,revoke_reason=? WHERE id=? AND user_id=? AND revoked_at IS NULL", time(now), reason, session, user);
    }
    @Override public void revokeOtherSessions(UUID user, UUID except, Instant now) {
        jdbc.update("UPDATE auth_sessions SET revoked_at=?,revoke_reason='USER_REVOKED' WHERE user_id=? AND id<>? AND revoked_at IS NULL", time(now), user, except);
    }
    @Override public void revokeAllSessions(UUID user, String reason, Instant now) {
        jdbc.update("UPDATE auth_sessions SET revoked_at=?,revoke_reason=? WHERE user_id=? AND revoked_at IS NULL", time(now), reason, user);
    }
    @Override public void createActionToken(UUID id, UUID user, byte[] hash, String purpose, Instant expires, Instant now) {
        jdbc.update("INSERT INTO auth_action_tokens(id,user_id,token_hash,purpose,expires_at,created_at) VALUES (?,?,?,?,?,?)",
                id, user, hash, purpose, time(expires), time(now));
    }
    @Override public Optional<UUID> consumeActionToken(byte[] hash, String purpose, Instant now) {
        List<UUID> ids = jdbc.query("""
                UPDATE auth_action_tokens SET consumed_at=? WHERE token_hash=? AND purpose=?
                AND consumed_at IS NULL AND expires_at>? RETURNING user_id
                """, (rs, row) -> uuid(rs, "user_id"), time(now), hash, purpose, time(now));
        return ids.stream().findFirst();
    }
    @Override public boolean actionTokenAlreadyConsumed(byte[] hash, String purpose) {
        Integer count = jdbc.queryForObject("""
                SELECT count(*) FROM auth_action_tokens WHERE token_hash=? AND purpose=? AND consumed_at IS NOT NULL
                """, Integer.class, hash, purpose);
        return count != null && count > 0;
    }
    @Override public boolean recordRateLimit(byte[] subject, Instant now, int maximum, long windowSeconds) {
        jdbc.update("""
                INSERT INTO auth_rate_limits(subject_hash,attempts,window_started_at,blocked_until) VALUES (?,1,?,NULL)
                ON CONFLICT (subject_hash) DO UPDATE SET
                  attempts=CASE WHEN auth_rate_limits.window_started_at<? THEN 1 ELSE auth_rate_limits.attempts+1 END,
                  window_started_at=CASE WHEN auth_rate_limits.window_started_at<? THEN ? ELSE auth_rate_limits.window_started_at END
                """, subject, time(now), time(now.minusSeconds(windowSeconds)), time(now.minusSeconds(windowSeconds)), time(now));
        Integer count = jdbc.queryForObject("SELECT attempts FROM auth_rate_limits WHERE subject_hash=?", Integer.class, subject);
        return count != null && count <= maximum;
    }
    @Override public void audit(UUID actor, String action, UUID target, Instant now) {
        jdbc.update("""
                INSERT INTO audit_logs(id,actor_user_id,actor_type,action,target_type,target_id,occurred_at)
                VALUES (?,?,'USER',?,'AUTH_SESSION',?,?)
                """, ids.next(), actor, action, target, time(now));
    }
    @Override public void purgeExpired(Instant now) {
        jdbc.update("DELETE FROM auth_action_tokens WHERE expires_at<? OR consumed_at<?",
                time(now.minusSeconds(7 * 86400L)), time(now.minusSeconds(86400)));
        jdbc.update("DELETE FROM auth_sessions WHERE expires_at<?", time(now.minusSeconds(7 * 86400L)));
        jdbc.update("DELETE FROM auth_rate_limits WHERE window_started_at<?", time(now.minusSeconds(86400)));
    }
}
