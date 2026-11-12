package com.mylog.reporting.application.query;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** {@code entryTitles} maps highlighted entry IDs to titles decrypted for the owner at read time. */
public record ReportView(UUID id, String type, LocalDate from, LocalDate to, String timezone,
                         int version, String status, int sampleSize, Map<String, Object> metrics,
                         String narrative, String generatedBy, List<ReportEvidenceView> evidence,
                         Map<String, String> entryTitles, Instant createdAt, Instant completedAt) {
    public ReportView withEntryTitles(Map<String, String> titles) {
        return new ReportView(id, type, from, to, timezone, version, status, sampleSize, metrics, narrative,
                generatedBy, evidence, Map.copyOf(titles), createdAt, completedAt);
    }
}
