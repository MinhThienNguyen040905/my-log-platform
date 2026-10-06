package com.mylog.insight.application.query;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InsightEvidenceView(String source, LocalDate date, String metric,
                                  BigDecimal value) {}
