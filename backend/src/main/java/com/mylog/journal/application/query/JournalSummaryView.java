package com.mylog.journal.application.query;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record JournalSummaryView(UUID id, String title, Instant occurredAt, LocalDate localDate,
                                 String moodCode, boolean favorite, String riskLevel,
                                 String analysisStatus, long version, List<JournalTagView> tags,
                                 String photoUrl) {
    public JournalSummaryView(UUID id, String title, Instant occurredAt, LocalDate localDate,
                             String moodCode, boolean favorite, String riskLevel,
                             String analysisStatus, long version, List<JournalTagView> tags) {
        this(id, title, occurredAt, localDate, moodCode, favorite, riskLevel, analysisStatus, version, tags, null);
    }

    public JournalSummaryView(UUID id, String title, Instant occurredAt, LocalDate localDate,
                             String moodCode, boolean favorite, String riskLevel,
                             String analysisStatus, long version) {
        this(id, title, occurredAt, localDate, moodCode, favorite, riskLevel, analysisStatus, version, List.of(), null);
    }
}
