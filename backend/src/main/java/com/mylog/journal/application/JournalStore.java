package com.mylog.journal.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JournalStore {
    void create(JournalEntrySnapshot entry);
    Optional<JournalEntrySnapshot> find(UUID userId, UUID entryId);
    List<JournalEntrySnapshot> list(UUID userId, LocalDate from, LocalDate to, UUID tagId,
                                    Boolean favorite, Instant cursorTime, UUID cursorId, int limit);
    boolean update(JournalEntrySnapshot entry, long expectedVersion);
    boolean setFavorite(UUID userId, UUID entryId, boolean favorite, Instant now);
    boolean softDelete(UUID userId, UUID entryId, long expectedVersion, Instant now);
    boolean transitionAnalysis(UUID userId, UUID entryId, int contentVersion,
                               List<String> from, String to, UUID analysisId, Instant now);
    boolean applyRescreen(UUID userId, UUID entryId, int contentVersion,
                          String riskLevel, String analysisStatus, Instant now);
    int purgeDeletedBefore(Instant cutoff);
}
