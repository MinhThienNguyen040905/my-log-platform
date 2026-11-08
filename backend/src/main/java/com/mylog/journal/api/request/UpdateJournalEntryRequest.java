package com.mylog.journal.api.request;

import com.mylog.journal.application.command.UpdateJournalEntryCommand;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UpdateJournalEntryRequest(String title, JsonNode contentJson, String location,
                                        Instant occurredAt, String timezone, String moodCode,
                                        BigDecimal moodScore, BigDecimal stressScore, BigDecimal energyScore,
                                        Integer sleepMinutes,
                                        @Size(max = 20) List<UUID> tagIds) {
    public UpdateJournalEntryCommand command() {
        return new UpdateJournalEntryCommand(title, contentJson, location, occurredAt, timezone, moodCode,
                moodScore, stressScore, energyScore, sleepMinutes, tagIds);
    }
}
