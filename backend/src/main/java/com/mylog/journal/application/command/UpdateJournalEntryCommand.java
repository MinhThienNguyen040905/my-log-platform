package com.mylog.journal.application.command;

import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;

public record UpdateJournalEntryCommand(String title, JsonNode contentJson, String location,
                                        Instant occurredAt, String timezone, String moodCode,
                                        BigDecimal moodScore, BigDecimal stressScore,
                                        BigDecimal energyScore, Integer sleepMinutes) {}
