package com.mylog.checkin.application.query;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CheckinView(UUID id, LocalDate localDate, String timezone, String moodCode,
                          BigDecimal moodScore, BigDecimal stressScore, BigDecimal energyScore,
                          Integer sleepMinutes, String note, List<ActivityView> activities,
                          long version, Instant createdAt, Instant updatedAt) {
    public record ActivityView(String code, Integer durationMinutes, String intensity) {}
}
