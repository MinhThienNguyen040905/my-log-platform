package com.mylog.journal.application.query;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record JournalSummaryView(UUID id, String title, Instant occurredAt, LocalDate localDate,
                                 String moodCode, boolean favorite, String riskLevel,
                                 String analysisStatus, long version) {}
