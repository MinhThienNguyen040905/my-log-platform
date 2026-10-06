package com.mylog.reporting.application;

import com.mylog.reporting.application.query.ReportPage;
import com.mylog.reporting.application.query.ReportView;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ReportStore {
    record UserZone(UUID userId, String timezone) {}
    record Task(UUID id, UUID userId, String type, LocalDate from, LocalDate to,
                String timezone, int version) {}
    record Evidence(String source, UUID sourceId, LocalDate date, String metric, BigDecimal value) {}
    record Result(int sampleSize, Map<String, Object> metrics, String narrative, List<Evidence> evidence) {}

    List<UserZone> usersAfter(UUID cursor, int limit);
    UUID enqueue(UUID userId, String type, LocalDate from, LocalDate to,
                 String timezone, boolean regenerate, Instant now);
    Task claim(String workerId, Instant now);
    void complete(UUID reportId, String workerId, Result result, Instant now);
    void fail(UUID reportId, String workerId, Instant now);
    ReportPage list(UUID userId, String type, String cursor, int limit);
    ReportView get(UUID userId, UUID reportId);
}
