package com.mylog.journal.api.response;

import com.mylog.journal.application.query.JournalEntryView;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record JournalEntryResponse(UUID id, String title, JsonNode contentJson, String location,
                                   Instant occurredAt, LocalDate localDate, String timezone, String moodCode,
                                   BigDecimal moodScore, BigDecimal stressScore, BigDecimal energyScore,
                                   Integer sleepMinutes, boolean favorite, String entryStatus, String riskLevel,
                                   String analysisStatus, int contentVersion, long rowVersion,
                                   Instant createdAt, Instant updatedAt) {
    public static JournalEntryResponse from(JournalEntryView view) {
        return new JournalEntryResponse(view.id(), view.title(), view.contentJson(), view.location(),
                view.occurredAt(), view.localDate(), view.timezone(), view.moodCode(), view.moodScore(),
                view.stressScore(), view.energyScore(), view.sleepMinutes(), view.favorite(),
                view.entryStatus(), view.riskLevel(), view.analysisStatus(), view.contentVersion(),
                view.version(), view.createdAt(), view.updatedAt());
    }
}
