package com.mylog.analysis.application;

import java.time.Instant;
import java.util.UUID;

public record AnalysisJob(
        UUID id,
        UUID userId,
        UUID journalId,
        long journalVersion,
        String jobType,
        int attemptCount,
        int maxAttempts,
        Instant startedAt) {}
