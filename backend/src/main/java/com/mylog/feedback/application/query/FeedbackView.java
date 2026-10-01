package com.mylog.feedback.application.query;
import java.time.Instant;
import java.util.UUID;
public record FeedbackView(UUID id,UUID userId,String category,String status,String message,UUID assignedTo,
                           Instant createdAt,Instant updatedAt,Instant resolvedAt,long version) {}
