package com.mylog.analysis.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Persistence port for thinking-pattern extractions and their signals (V22). */
public interface PatternSignalStore {
    /** A saved, not deleted, ALLOW-screened entry whose current version has not been read for the taxonomy. */
    record Candidate(UUID entryId, int contentVersion, LocalDate localDate) {}

    String timezone(UUID userId);

    /** Newest first, at most {@code limit}. */
    List<Candidate> pending(UUID userId, LocalDate from, LocalDate to, String taxonomyVersion, int limit);

    /** Drops extractions of entries that were deleted, edited since, or are no longer screened ALLOW. */
    int purgeStale(UUID userId);

    /**
     * Stores the extraction and its accepted signals (quotes encrypted). Returns false, storing nothing, when
     * the entry changed meanwhile or this version was already stored.
     */
    boolean save(UUID userId, Candidate candidate, String taxonomyVersion, PatternExtractor.Result result,
                 List<PatternExtractor.Signal> accepted, Instant now);

    /** Provider metadata only (operation PATTERN_EXTRACTION); never prompt, text or quotes. */
    void recordUsage(PatternExtractor.Result result, long latencyMs, Instant now);
}
