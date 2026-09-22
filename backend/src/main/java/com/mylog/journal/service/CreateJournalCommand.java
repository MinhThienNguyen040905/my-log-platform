package com.mylog.journal.service;

import com.mylog.journal.entity.ContentFormat;
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
