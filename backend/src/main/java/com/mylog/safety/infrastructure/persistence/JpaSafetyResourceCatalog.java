package com.mylog.safety.infrastructure.persistence;

import com.mylog.safety.application.SafetyResourceCatalog;
import com.mylog.safety.application.query.SafetyResourceView;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JpaSafetyResourceCatalog implements SafetyResourceCatalog {
    private final EntityManager entityManager;

    JpaSafetyResourceCatalog(EntityManager entityManager) { this.entityManager = entityManager; }

    @Override public List<SafetyResourceView> approvedFor(String locale, String countryCode) {
        return entityManager.createQuery("""
                select r from SafetyResource r where r.locale=:locale and r.countryCode=:countryCode
                  and r.status='APPROVED' and r.verifiedAt is not null and r.sourceUrl like 'https://%'
                order by r.name
                """, SafetyResource.class).setParameter("locale", locale)
                .setParameter("countryCode", countryCode).getResultList().stream()
                .map(r -> new SafetyResourceView(r.id, r.name, r.resourceType, r.contactValue,
                        r.description, r.sourceUrl, r.version)).toList();
    }
}
