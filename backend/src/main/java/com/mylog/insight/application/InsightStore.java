package com.mylog.insight.application;

import com.mylog.insight.application.query.InsightPage;
import com.mylog.insight.domain.MoodSleepAssociation;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public interface InsightStore {
    void saveAssociation(UUID userId, LocalDate from, LocalDate to, String timezone,
                         MoodSleepAssociation.Result result, Instant now);
    InsightPage list(UUID userId, LocalDate from, LocalDate to, String cursor, int limit);
}
