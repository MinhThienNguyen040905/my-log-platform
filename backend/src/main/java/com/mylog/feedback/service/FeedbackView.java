package com.mylog.feedback.service;

import java.time.Instant;
import java.util.UUID;

public record FeedbackView(
        UUID id, String targetType, UUID targetId, String value, Instant createdAt, Instant updatedAt) {}
