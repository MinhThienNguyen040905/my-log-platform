package com.mylog.journal.application.query;

import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record JournalEntryView(UUID id, String title, JsonNode contentJson, String location,
                               Instant occurredAt, LocalDate localDate, String timezone,
                               String moodCode, BigDecimal moodScore, BigDecimal stressScore,
                               BigDecimal energyScore, Integer sleepMinutes, boolean favorite,
                               String entryStatus, String riskLevel, String analysisStatus,
                               int contentVersion, long version, Instant createdAt, Instant updatedAt,
                               List<JournalTagView> tags, String photoUrl) {
    public JournalEntryView(UUID id, String title, JsonNode contentJson, String location,
                            Instant occurredAt, LocalDate localDate, String timezone,
                            String moodCode, BigDecimal moodScore, BigDecimal stressScore,
                            BigDecimal energyScore, Integer sleepMinutes, boolean favorite,
                            String entryStatus, String riskLevel, String analysisStatus,
                            int contentVersion, long version, Instant createdAt, Instant updatedAt,
                            List<JournalTagView> tags) {
        this(id, title, contentJson, location, occurredAt, localDate, timezone, moodCode, moodScore,
                stressScore, energyScore, sleepMinutes, favorite, entryStatus, riskLevel,
                analysisStatus, contentVersion, version, createdAt, updatedAt, tags, null);
    }

    public JournalEntryView(UUID id, String title, JsonNode contentJson, String location,
                            Instant occurredAt, LocalDate localDate, String timezone,
                            String moodCode, BigDecimal moodScore, BigDecimal stressScore,
                            BigDecimal energyScore, Integer sleepMinutes, boolean favorite,
                            String entryStatus, String riskLevel, String analysisStatus,
                            int contentVersion, long version, Instant createdAt, Instant updatedAt) {
        this(id, title, contentJson, location, occurredAt, localDate, timezone, moodCode, moodScore,
                stressScore, energyScore, sleepMinutes, favorite, entryStatus, riskLevel,
                analysisStatus, contentVersion, version, createdAt, updatedAt, List.of(), null);
    }
}
