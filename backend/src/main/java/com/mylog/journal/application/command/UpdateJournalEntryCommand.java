package com.mylog.journal.application.command;

import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UpdateJournalEntryCommand(String title, JsonNode contentJson, String location,
                                        Instant occurredAt, String timezone, String moodCode,
                                        BigDecimal moodScore, BigDecimal stressScore,
                                        BigDecimal energyScore, Integer sleepMinutes,
                                        List<UUID> tagIds) {
    public UpdateJournalEntryCommand(String title, JsonNode contentJson, String location,
                                    Instant occurredAt, String timezone, String moodCode,
                                    BigDecimal moodScore, BigDecimal stressScore,
                                    BigDecimal energyScore, Integer sleepMinutes) {
        this(title, contentJson, location, occurredAt, timezone, moodCode, moodScore, stressScore, energyScore, sleepMinutes, null);
    }
}
