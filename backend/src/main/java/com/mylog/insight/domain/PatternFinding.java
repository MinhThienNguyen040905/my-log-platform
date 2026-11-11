package com.mylog.insight.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One statistical pattern found in a user's recent data. {@code facts} holds codes and numbers only
 * (never user-written text such as tag names) because it is stored as the plaintext metrics snapshot.
 */
public record PatternFinding(String type, String direction, BigDecimal strength, int sampleSize,
                             Map<String, Object> facts, List<EvidencePoint> evidence) {
    /** {@code sourceType} is CHECKIN, JOURNAL or STRUCTURED_METRIC, matching insight_evidence. */
    public record EvidencePoint(LocalDate date, String sourceType, UUID sourceId, String metric, BigDecimal value) {}
}
