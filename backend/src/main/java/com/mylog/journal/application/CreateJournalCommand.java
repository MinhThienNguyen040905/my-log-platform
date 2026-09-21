package com.mylog.journal.application;

import com.mylog.journal.domain.ContentFormat;
import java.time.Instant;
import java.util.Map;

public record CreateJournalCommand(
        String title,
        String contentText,
        Map<String, Object> contentJson,
        ContentFormat contentFormat,
        int moodScore,
        Integer stressScore,
        Integer energyScore,
        Instant occurredAt,
        String timezoneAtEntry,
        boolean favorite) {}
