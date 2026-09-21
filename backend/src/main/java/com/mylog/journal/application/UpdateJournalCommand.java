package com.mylog.journal.application;

import com.mylog.journal.domain.ContentFormat;
import java.time.Instant;
import java.util.Map;

public record UpdateJournalCommand(
        long version,
        Value<String> title,
        Value<String> contentText,
        Value<Map<String, Object>> contentJson,
        Value<ContentFormat> contentFormat,
        Value<Integer> moodScore,
        Value<Integer> stressScore,
        Value<Integer> energyScore,
        Value<Instant> occurredAt,
        Value<String> timezoneAtEntry,
        Value<Boolean> favorite) {

    public record Value<T>(boolean supplied, T value) {}
}
