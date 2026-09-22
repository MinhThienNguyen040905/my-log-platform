package com.mylog.journal.service;

import com.mylog.common.messaging.MessagingTopology;
import com.mylog.common.outbox.OutboxWriter;
import com.mylog.journal.entity.JournalEntry;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class JournalEventPublisher {

    private final OutboxWriter outboxWriter;

    JournalEventPublisher(OutboxWriter outboxWriter) {
        this.outboxWriter = outboxWriter;
    }

    void journalChanged(JournalEntry entry, String eventType) {
        outboxWriter.append("JOURNAL", entry.getId(), eventType, 1, payload(entry));
    }

    void analysisRequested(JournalEntry entry) {
        outboxWriter.append(
                "JOURNAL", entry.getId(), MessagingTopology.JOURNAL_ANALYSIS_REQUESTED, 1, payload(entry));
    }

    private Map<String, Object> payload(JournalEntry entry) {
        return Map.of(
                "journalId", entry.getId().toString(),
                "userId", entry.getUserId().toString(),
                "journalVersion", entry.getJournalVersion());
    }
}
