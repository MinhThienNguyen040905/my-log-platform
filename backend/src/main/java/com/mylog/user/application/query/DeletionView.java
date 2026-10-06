package com.mylog.user.application.query;
import java.time.Instant;
import java.util.UUID;
public record DeletionView(UUID id, String status, Instant requestedAt, Instant scheduledFor, Instant completedAt) {}
