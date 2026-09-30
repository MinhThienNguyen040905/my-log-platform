package com.mylog.checkin.application;

import com.mylog.checkin.application.command.PutCheckinCommand;
import com.mylog.checkin.application.query.CheckinView;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckinStore {
    CheckinView put(UUID userId, LocalDate date, PutCheckinCommand command, Instant now);
    Optional<CheckinView> find(UUID userId, LocalDate date);
    List<CheckinView> list(UUID userId, LocalDate from, LocalDate to);
}
