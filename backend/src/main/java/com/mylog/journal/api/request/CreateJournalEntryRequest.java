package com.mylog.journal.api.request;

import com.mylog.journal.application.command.CreateJournalEntryCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateJournalEntryRequest(String title, @NotNull JsonNode contentJson, String location,
                                        Instant occurredAt, @NotBlank String timezone, String moodCode,
                                        BigDecimal moodScore, BigDecimal stressScore, BigDecimal energyScore,
                                        Integer sleepMinutes) {
    public CreateJournalEntryCommand command() {
        return new CreateJournalEntryCommand(title, contentJson, location, occurredAt, timezone, moodCode,
                moodScore, stressScore, energyScore, sleepMinutes);
    }
}
