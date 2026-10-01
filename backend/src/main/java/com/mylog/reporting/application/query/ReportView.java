package com.mylog.reporting.application.query;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ReportView(UUID id, String type, LocalDate from, LocalDate to, String timezone,
                         int version, String status, int sampleSize, Map<String, Object> metrics,
                         String narrative, List<ReportEvidenceView> evidence,
                         Instant createdAt, Instant completedAt) {}
