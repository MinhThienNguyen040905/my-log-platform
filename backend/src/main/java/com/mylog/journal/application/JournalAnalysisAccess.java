package com.mylog.journal.application;

import java.time.Instant;
import java.util.UUID;

/** Public feature boundary for asynchronous analysis. */
public interface JournalAnalysisAccess {
    record Input(UUID userId, UUID entryId, int contentVersion, String title, String plainText) {}
    Input load(UUID userId, UUID entryId, int contentVersion);
    /**
     * Text of a saved, not deleted entry at exactly {@code contentVersion} whose screening allowed ordinary AI
     * processing (risk NORMAL or LOW), whatever its analysis status; null otherwise. Used by thinking-pattern
     * extraction, which reads entries that may already be analyzed.
     */
    Input loadForPatterns(UUID userId, UUID entryId, int contentVersion);
    boolean start(UUID userId, UUID entryId, int contentVersion, Instant now);
    boolean activate(UUID userId, UUID entryId, int contentVersion, UUID analysisId, Instant now);
    void fail(UUID userId, UUID entryId, int contentVersion, Instant now);
    void block(UUID userId, UUID entryId, int contentVersion, Instant now);
    void cancel(UUID userId, UUID entryId, int contentVersion, Instant now);
    boolean retry(UUID userId, UUID entryId, int contentVersion, Instant now);
}
