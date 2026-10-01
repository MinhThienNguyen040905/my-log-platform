package com.mylog.journal.infrastructure.persistence;

import com.mylog.journal.application.JournalService;
import com.mylog.platform.outbox.OutboxHandler;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "mylog.jobs", name = "enabled", havingValue = "true")
class JournalSafetyOutboxHandler implements OutboxHandler {
    private final EntityManager em;
    private final JournalService journal;

    JournalSafetyOutboxHandler(EntityManager em, JournalService journal) {
        this.em = em; this.journal = journal;
    }

    @Override public boolean supports(String eventType) { return "SafetyRescreenRequested".equals(eventType); }

    @Override public void handle(String eventType, UUID entryId, int version) {
        @SuppressWarnings("unchecked")
        List<UUID> owners = em.createNativeQuery("""
                SELECT user_id FROM journal_entries WHERE id=?1 AND content_version=?2 AND deleted_at IS NULL
                """, UUID.class).setParameter(1, entryId).setParameter(2, version).getResultList();
        if (owners.isEmpty()) return;
        if (!journal.rescreen(owners.getFirst(), entryId, version))
            throw new IllegalStateException("POLICY_NOT_READY");
    }
}
