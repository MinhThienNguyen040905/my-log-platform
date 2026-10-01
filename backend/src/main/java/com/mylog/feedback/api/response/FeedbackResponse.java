package com.mylog.feedback.api.response;
import com.mylog.feedback.application.query.FeedbackView;
import java.time.Instant;
import java.util.UUID;
public record FeedbackResponse(UUID id,UUID userId,String category,String status,String message,
                               UUID assignedTo,Instant createdAt,Instant updatedAt,Instant resolvedAt,long version) {
    public static FeedbackResponse from(FeedbackView view) {
        return new FeedbackResponse(view.id(),view.userId(),view.category(),view.status(),view.message(),
                view.assignedTo(),view.createdAt(),view.updatedAt(),view.resolvedAt(),view.version());
    }
}
