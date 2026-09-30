package com.mylog.journal.api.response;

import com.mylog.journal.application.query.JournalSummaryView;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record JournalSummaryResponse(UUID id, String title, Instant occurredAt, LocalDate localDate,
                                     String moodCode, boolean favorite, String riskLevel,
                                     String analysisStatus, long rowVersion) {
    public static JournalSummaryResponse from(JournalSummaryView view) {
        return new JournalSummaryResponse(view.id(), view.title(), view.occurredAt(), view.localDate(),
                view.moodCode(), view.favorite(), view.riskLevel(), view.analysisStatus(), view.version());
    }
}
