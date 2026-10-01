package com.mylog.insight.application.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InsightView(UUID id, String type, LocalDate from, LocalDate to, String timezone,
                          String direction, BigDecimal strength, int sampleSize,
                          String algorithmVersion, String narrative,
                          List<InsightEvidenceView> evidence) {}
