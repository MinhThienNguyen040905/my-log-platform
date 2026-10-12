package com.mylog.identity.infrastructure.persistence;

import com.mylog.identity.application.DevAccountStore;
import com.mylog.identity.infrastructure.persistence.entity.User;
import com.mylog.platform.crypto.SensitiveDataCipher;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@Profile("local & !staging & !prod")
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JpaDevAccountStore implements DevAccountStore {
    private final EntityManager em;

    JpaDevAccountStore(EntityManager em) { this.em = em; }

    @Override public List<Account> list(UUID beforeId, int limit) {
        return em.createQuery("select u from User u where u.status='ACTIVE' and u.deletedAt is null "
                        + "and (:before is null or u.id < :before) order by u.id desc", User.class)
                .setParameter("before", beforeId)
                .setMaxResults(limit)
                .getResultList().stream()
                .map(user -> new Account(user.id, user.status,
                        new SensitiveDataCipher.Encrypted(user.encryptedEmail, user.emailIv,
                                user.emailWrappedKey, user.emailKeyVersion)))
                .toList();
    }
}
