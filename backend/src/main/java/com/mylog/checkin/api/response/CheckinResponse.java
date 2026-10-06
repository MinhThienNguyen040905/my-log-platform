package com.mylog.checkin.api.response;

import com.mylog.checkin.application.query.CheckinView;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CheckinResponse(UUID id, LocalDate localDate, String timezone, String moodCode,
                              BigDecimal moodScore, BigDecimal stressScore, BigDecimal energyScore,
                              Integer sleepMinutes, String note, List<ActivityResponse> activities,
                              long version, Instant createdAt, Instant updatedAt) {
    public record ActivityResponse(String code, Integer durationMinutes, String intensity) {}

    public static CheckinResponse from(CheckinView view) {
        return new CheckinResponse(view.id(), view.localDate(), view.timezone(), view.moodCode(),
                view.moodScore(), view.stressScore(), view.energyScore(), view.sleepMinutes(), view.note(),
                view.activities().stream().map(a -> new ActivityResponse(a.code(), a.durationMinutes(), a.intensity())).toList(),
                view.version(), view.createdAt(), view.updatedAt());
    }
}
