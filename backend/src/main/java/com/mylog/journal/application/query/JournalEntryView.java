package com.mylog.journal.application.query;

import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record JournalEntryView(UUID id, String title, JsonNode contentJson, String location,
                               Instant occurredAt, LocalDate localDate, String timezone,
                               String moodCode, BigDecimal moodScore, BigDecimal stressScore,
                               BigDecimal energyScore, Integer sleepMinutes, boolean favorite,
                               String entryStatus, String riskLevel, String analysisStatus,
                               int contentVersion, long version, Instant createdAt, Instant updatedAt) {}
