package com.mylog.analysis.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Local-only persistence port for {@link DevAnalysisSeedService}. */
public interface DevAnalysisSeedStore {
    record Candidate(UUID entryId, int contentVersion, LocalDate localDate, String moodCode,
                     BigDecimal moodScore, BigDecimal stressScore, Integer sleepMinutes, List<UUID> tagIds) {}

    String timezone(UUID userId);

    /** Saved, non-deleted entries of the user in the window whose analysis is not current. */
    List<Candidate> candidates(UUID userId, LocalDate from, LocalDate to);

    UUID save(UUID userId, UUID entryId, int contentVersion, JournalAnalyzer.Result result, Instant now);
}
