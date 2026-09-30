package com.mylog.user.infrastructure;

import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.user.application.UserStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class UserRepository implements UserStore {
    private final JdbcTemplate jdbc;
    UserRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    private static Timestamp time(Instant value) { return value == null ? null : Timestamp.from(value); }

    @Override public void create(Profile p, Instant now) {
        jdbc.update("""
                INSERT INTO user_profiles(user_id,encrypted_profile,profile_iv,profile_wrapped_key,profile_key_version,
                timezone,locale,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?)
                """, p.userId(), p.payload().ciphertext(), p.payload().iv(), p.payload().wrappedKey(),
                p.payload().keyVersion(), p.timezone(), p.locale(), time(now), time(now));
    }
    @Override public Optional<Profile> find(UUID id) {
        return jdbc.query("SELECT * FROM user_profiles WHERE user_id=?", (rs, row) -> new Profile(
                (UUID) rs.getObject("user_id"),
                new SensitiveDataCipher.Encrypted(rs.getBytes("encrypted_profile"), rs.getBytes("profile_iv"),
                        rs.getBytes("profile_wrapped_key"), rs.getString("profile_key_version")),
                rs.getString("timezone"), rs.getString("locale"),
                rs.getTimestamp("onboarding_completed_at") == null ? null : rs.getTimestamp("onboarding_completed_at").toInstant(),
                rs.getLong("row_version")), id).stream().findFirst();
    }
    @Override public boolean update(Profile p, long expected, Instant now) {
        return jdbc.update("""
                UPDATE user_profiles SET encrypted_profile=?,profile_iv=?,profile_wrapped_key=?,profile_key_version=?,
                timezone=?,locale=?,updated_at=?,row_version=row_version+1 WHERE user_id=? AND row_version=?
                """, p.payload().ciphertext(), p.payload().iv(), p.payload().wrappedKey(), p.payload().keyVersion(),
                p.timezone(), p.locale(), time(now), p.userId(), expected) == 1;
    }
    @Override public boolean completeOnboarding(UUID user, long expected, Instant now) {
        return jdbc.update("""
                UPDATE user_profiles SET onboarding_completed_at=COALESCE(onboarding_completed_at,?),
                updated_at=?,row_version=row_version+1 WHERE user_id=? AND row_version=?
                """, time(now), time(now), user, expected) == 1;
    }
    @Override public void addConsent(UUID id, UUID user, String type, String version,
                                     boolean granted, String source, Instant now) {
        jdbc.update("""
                INSERT INTO user_consents(id,user_id,consent_type,document_version,granted,decided_at,source)
                VALUES (?,?,?,?,?,?,?)
                """, id, user, type, version, granted, time(now), source);
    }
    @Override public List<Consent> latestConsents(UUID user) {
        return jdbc.query("""
                SELECT DISTINCT ON (consent_type) consent_type,document_version,granted,decided_at
                FROM user_consents WHERE user_id=? ORDER BY consent_type,decided_at DESC,id DESC
                """, (rs, row) -> new Consent(rs.getString("consent_type"), rs.getString("document_version"),
                rs.getBoolean("granted"), rs.getTimestamp("decided_at").toInstant()), user);
    }
}
