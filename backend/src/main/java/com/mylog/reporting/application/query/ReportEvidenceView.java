package com.mylog.reporting.application.query;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReportEvidenceView(String source, LocalDate date, String metric, BigDecimal value) {}
