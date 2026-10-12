package com.mylog.journal.infrastructure.persistence;

import com.mylog.journal.application.JournalService;
import com.mylog.platform.outbox.OutboxHandler;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "mylog.jobs", name = "enabled", havingValue = "true")
class JournalSafetyOutboxHandler implements OutboxHandler {
    private final EntityManager em;
    private final JournalService journal;
    private final boolean mvpEnabled;

    JournalSafetyOutboxHandler(EntityManager em, JournalService journal,
                               @Value("${mylog.ai.mvp-enabled:false}") boolean mvpEnabled) {
        this.em = em; this.journal = journal; this.mvpEnabled = mvpEnabled;
    }

    @Override public boolean supports(String eventType) { return "SafetyRescreenRequested".equals(eventType); }

    @Override public void handle(String eventType, UUID entryId, int version) {
        // Enabling the MVP must not reclassify older blocked journals in a shared database.
        if (mvpEnabled) throw new IllegalStateException("POLICY_NOT_READY");
        @SuppressWarnings("unchecked")
        List<UUID> owners = em.createNativeQuery("""
                SELECT user_id FROM journal_entries WHERE id=?1 AND content_version=?2 AND deleted_at IS NULL
                """, UUID.class).setParameter(1, entryId).setParameter(2, version).getResultList();
        if (owners.isEmpty()) return;
        if (!journal.rescreen(owners.getFirst(), entryId, version))
            throw new IllegalStateException("POLICY_NOT_READY");
    }
}
