package com.mylog.analysis.application;

import java.time.Instant;
import java.util.UUID;

public interface AnalysisStore {
    boolean ownsLease(UUID jobId, String workerId, Instant now);
    UUID save(UUID jobId, UUID userId, UUID entryId, int contentVersion,
              String safetyPolicyVersion, JournalAnalyzer.Result result, long latencyMs,
              boolean current, Instant now);
    void markStale(UUID analysisId);
}
