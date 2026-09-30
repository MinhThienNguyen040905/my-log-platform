package com.mylog.user.infrastructure.persistence;

import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.user.application.UserStore;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class UserRepository implements UserStore {
    private final EntityManager entityManager;

    UserRepository(EntityManager entityManager) { this.entityManager = entityManager; }

    @Override public void create(Profile profile, Instant now) {
        UserProfileEntity entity = new UserProfileEntity();
        entity.userId = profile.userId();
        entity.encryptedProfile = profile.payload().ciphertext();
        entity.profileIv = profile.payload().iv();
        entity.profileWrappedKey = profile.payload().wrappedKey();
        entity.profileKeyVersion = profile.payload().keyVersion();
        entity.timezone = profile.timezone();
        entity.locale = profile.locale();
        entity.createdAt = now;
        entity.updatedAt = now;
        entityManager.persist(entity);
    }

    @Override public Optional<Profile> find(UUID userId) {
        return Optional.ofNullable(entityManager.find(UserProfileEntity.class, userId)).map(this::profile);
    }

    @Override public boolean update(Profile profile, long expectedVersion, Instant now) {
        int changed = entityManager.createQuery("""
                update UserProfileEntity p set p.encryptedProfile=:ciphertext, p.profileIv=:iv,
                  p.profileWrappedKey=:wrappedKey, p.profileKeyVersion=:keyVersion,
                  p.timezone=:timezone, p.locale=:locale, p.updatedAt=:now,
                  p.rowVersion=p.rowVersion+1
                where p.userId=:userId and p.rowVersion=:expectedVersion
                """)
                .setParameter("ciphertext", profile.payload().ciphertext())
                .setParameter("iv", profile.payload().iv())
                .setParameter("wrappedKey", profile.payload().wrappedKey())
                .setParameter("keyVersion", profile.payload().keyVersion())
                .setParameter("timezone", profile.timezone())
                .setParameter("locale", profile.locale())
                .setParameter("now", now)
                .setParameter("userId", profile.userId())
                .setParameter("expectedVersion", expectedVersion)
                .executeUpdate();
        entityManager.clear();
        return changed == 1;
    }

    @Override public boolean completeOnboarding(UUID userId, long expectedVersion, Instant now) {
        int changed = entityManager.createQuery("""
                update UserProfileEntity p set p.onboardingCompletedAt=coalesce(p.onboardingCompletedAt,:now),
                  p.updatedAt=:now, p.rowVersion=p.rowVersion+1
                where p.userId=:userId and p.rowVersion=:expectedVersion
                """)
                .setParameter("now", now)
                .setParameter("userId", userId)
                .setParameter("expectedVersion", expectedVersion)
                .executeUpdate();
        entityManager.clear();
        return changed == 1;
    }

    @Override public void addConsent(UUID id, UUID userId, String type, String version,
                                     boolean granted, String source, Instant now) {
        UserConsentEntity entity = new UserConsentEntity();
        entity.id = id;
        entity.userId = userId;
        entity.consentType = type;
        entity.documentVersion = version;
        entity.granted = granted;
        entity.decidedAt = now;
        entity.source = source;
        entityManager.persist(entity);
    }

    @Override public List<Consent> latestConsents(UUID userId) {
        @SuppressWarnings("unchecked")
        List<UserConsentEntity> latest = entityManager.createNativeQuery("""
                SELECT DISTINCT ON (consent_type) * FROM user_consents
                WHERE user_id=?1 ORDER BY consent_type, decided_at DESC, id DESC
                """, UserConsentEntity.class).setParameter(1, userId).getResultList();
        return latest.stream().map(entity -> new Consent(entity.consentType, entity.documentVersion,
                entity.granted, entity.decidedAt)).toList();
    }

    private Profile profile(UserProfileEntity entity) {
        return new Profile(entity.userId,
                new SensitiveDataCipher.Encrypted(entity.encryptedProfile, entity.profileIv,
                        entity.profileWrappedKey, entity.profileKeyVersion), entity.timezone, entity.locale,
                entity.onboardingCompletedAt, entity.rowVersion);
    }
}
