package com.mylog.journal.application;

import com.mylog.platform.crypto.SensitiveDataCipher;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record JournalEntrySnapshot(UUID id, UUID userId, SensitiveDataCipher.Encrypted payload,
                                   Instant occurredAt, LocalDate localDate, String timezone,
                                   String moodCode, BigDecimal moodScore, BigDecimal stressScore,
                                   BigDecimal energyScore, Integer sleepMinutes, boolean favorite,
                                   String entryStatus, String riskLevel, String analysisStatus,
                                   int contentVersion, long rowVersion, Instant createdAt,
                                   Instant updatedAt, Instant deletedAt) {}
