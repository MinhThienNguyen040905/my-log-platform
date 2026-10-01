package com.mylog.safety.application;

import java.time.Instant;
import java.util.UUID;

public interface SafetyEventRecorder {
    void record(UUID userId, UUID entryId, SafetyDecision decision, Instant now);
}
