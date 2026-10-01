package com.mylog.insight.application.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DailyMetric(LocalDate date, String source, UUID sourceId, int journalCount,
                          BigDecimal moodScore, BigDecimal stressScore, BigDecimal energyScore,
                          Integer sleepMinutes) {}
