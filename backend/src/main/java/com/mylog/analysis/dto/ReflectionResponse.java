package com.mylog.analysis.dto;

import com.mylog.analysis.service.ReflectionView;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReflectionResponse(
        UUID journalId,
        long journalVersion,
        UUID generationBatchId,
        List<Question> questions) {

    public record Question(UUID id, int position, String question, Instant createdAt) {}

    public static ReflectionResponse from(ReflectionView view) {
        return new ReflectionResponse(
                view.journalId(), view.journalVersion(), view.generationBatchId(),
                view.questions().stream().map(item -> new Question(
                        item.id(), item.position(), item.question(), item.createdAt())).toList());
    }
}
