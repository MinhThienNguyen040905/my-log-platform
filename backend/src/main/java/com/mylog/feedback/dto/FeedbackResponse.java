package com.mylog.feedback.dto;

import com.mylog.feedback.service.FeedbackView;
import java.time.Instant;
import java.util.UUID;

public record FeedbackResponse(
        UUID id, String targetType, UUID targetId, String value, Instant createdAt, Instant updatedAt) {

    public static FeedbackResponse from(FeedbackView view) {
        return new FeedbackResponse(
                view.id(), view.targetType(), view.targetId(), view.value(), view.createdAt(), view.updatedAt());
    }
}
