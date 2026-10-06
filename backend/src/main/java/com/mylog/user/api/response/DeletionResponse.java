package com.mylog.user.api.response;
import com.mylog.user.application.query.DeletionView;
import java.time.Instant;
import java.util.UUID;
public record DeletionResponse(UUID id,String status,Instant requestedAt,Instant scheduledFor,Instant completedAt) {
    public static DeletionResponse from(DeletionView view) {
        return new DeletionResponse(view.id(),view.status(),view.requestedAt(),view.scheduledFor(),view.completedAt());
    }
}
