package com.mylog.analysis.infrastructure.persistence;

import com.mylog.platform.id.IdGenerator;
import com.mylog.platform.outbox.OutboxHandler;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "mylog.jobs", name = "enabled", havingValue = "true")
class JournalAnalysisOutboxHandler implements OutboxHandler {
    private final EntityManager em;
    private final IdGenerator ids;
    private final Clock clock;

    JournalAnalysisOutboxHandler(EntityManager em, IdGenerator ids, Clock clock) {
        this.em = em; this.ids = ids; this.clock = clock;
    }

    @Override public boolean supports(String eventType) {
        return "JournalEntrySubmitted".equals(eventType) || "JournalEntryChanged".equals(eventType)
                || "JournalEntryDeleted".equals(eventType);
    }

    @Override public void handle(String eventType, UUID entryId, int version) {
        if (version < 1) return;
        String invalidate = "JournalEntryDeleted".equals(eventType)
                ? "UPDATE ai_analyses SET status='STALE' WHERE journal_entry_id=?1 AND content_version<=?2 AND status='SUCCEEDED'"
                : "UPDATE ai_analyses SET status='STALE' WHERE journal_entry_id=?1 AND content_version<?2 AND status='SUCCEEDED'";
        em.createNativeQuery(invalidate)
                .setParameter(1, entryId).setParameter(2, version).executeUpdate();
        if (!"JournalEntrySubmitted".equals(eventType)) return;
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT user_id, content_version, analysis_status FROM journal_entries
                WHERE id=?1 AND deleted_at IS NULL
                """).setParameter(1, entryId).getResultList();
        if (rows.isEmpty() || ((Number) rows.getFirst()[1]).intValue() != version) return;
        if (!List.of("PENDING", "ANALYSIS_OUTDATED").contains(rows.getFirst()[2].toString())) return;
        UUID jobId = ids.next();
        em.createNativeQuery("""
                INSERT INTO ai_jobs (id,job_type,aggregate_type,aggregate_id,user_id,status,
                    priority,attempt,max_attempts,available_at,idempotency_key,payload,payload_version,created_at)
                VALUES (?1,'JOURNAL_ANALYSIS','JOURNAL_ENTRY',?2,?3,'PENDING',100,0,5,?4,?5,
                    jsonb_build_object('aggregateId',cast(?2 as text),'contentVersion',?6),1,?4)
                ON CONFLICT (idempotency_key) DO NOTHING
                """).setParameter(1, jobId).setParameter(2, entryId).setParameter(3, rows.getFirst()[0])
                .setParameter(4, clock.instant())
                .setParameter(5, "journal-analysis:" + entryId + ":" + version)
                .setParameter(6, version).executeUpdate();
    }
}
