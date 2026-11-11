package com.mylog.insight.application.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Structured signals of one saved journal entry; emotions/topics are empty until it is ANALYZED. */
public record EntrySignal(UUID entryId, LocalDate date, BigDecimal moodScore, BigDecimal stressScore,
                          List<String> emotions, List<String> topics, List<UUID> tagIds) {}
