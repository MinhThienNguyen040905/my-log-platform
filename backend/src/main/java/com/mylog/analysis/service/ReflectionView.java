package com.mylog.analysis.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReflectionView(
        UUID journalId,
        long journalVersion,
        UUID generationBatchId,
        List<Question> questions) {

    public record Question(UUID id, int position, String question, Instant createdAt) {}
}
