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
        UserProfile userProfile = new UserProfile();
        userProfile.userId = profile.userId();
        userProfile.encryptedProfile = profile.payload().ciphertext();
        userProfile.profileIv = profile.payload().iv();
        userProfile.profileWrappedKey = profile.payload().wrappedKey();
        userProfile.profileKeyVersion = profile.payload().keyVersion();
        userProfile.timezone = profile.timezone();
        userProfile.locale = profile.locale();
        userProfile.createdAt = now;
        userProfile.updatedAt = now;
        entityManager.persist(userProfile);
    }

    @Override public Optional<Profile> find(UUID userId) {
        return Optional.ofNullable(entityManager.find(UserProfile.class, userId)).map(this::profile);
    }

    @Override public boolean update(Profile profile, long expectedVersion, Instant now) {
        int changed = entityManager.createQuery("""
                update UserProfile p set p.encryptedProfile=:ciphertext, p.profileIv=:iv,
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
                update UserProfile p set p.onboardingCompletedAt=coalesce(p.onboardingCompletedAt,:now),
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
        UserConsent consent = new UserConsent();
        consent.id = id;
        consent.userId = userId;
        consent.consentType = type;
        consent.documentVersion = version;
        consent.granted = granted;
        consent.decidedAt = now;
        consent.source = source;
        entityManager.persist(consent);
    }

    @Override public List<Consent> latestConsents(UUID userId) {
        @SuppressWarnings("unchecked")
        List<UserConsent> latest = entityManager.createNativeQuery("""
                SELECT DISTINCT ON (consent_type) * FROM user_consents
                WHERE user_id=?1 ORDER BY consent_type, decided_at DESC, id DESC
                """, UserConsent.class).setParameter(1, userId).getResultList();
        return latest.stream().map(consent -> new Consent(consent.consentType, consent.documentVersion,
                consent.granted, consent.decidedAt)).toList();
    }

    private Profile profile(UserProfile userProfile) {
        return new Profile(userProfile.userId,
                new SensitiveDataCipher.Encrypted(userProfile.encryptedProfile, userProfile.profileIv,
                        userProfile.profileWrappedKey, userProfile.profileKeyVersion), userProfile.timezone, userProfile.locale,
                userProfile.onboardingCompletedAt, userProfile.rowVersion);
    }
}
