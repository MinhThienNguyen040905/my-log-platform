package com.mylog.analysis.application;

import java.time.Instant;
import java.util.UUID;

public interface AnalysisRetryStore {
    enum State { QUEUED, ALREADY_QUEUED, TOO_SOON, MISSING }
    State requeue(UUID userId, UUID entryId, int contentVersion, Instant now);
}
