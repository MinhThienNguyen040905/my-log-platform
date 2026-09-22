package com.mylog.journal.dto;

import com.mylog.journal.entity.ContentFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;

public record CreateJournalRequest(
        @Size(max = 200) String title,
        @NotBlank @Size(max = 100_000) String contentText,
        Map<String, Object> contentJson,
        @NotNull ContentFormat contentFormat,
        @NotNull @Min(1) @Max(10) Integer moodScore,
        @Min(1) @Max(10) Integer stressScore,
        @Min(1) @Max(10) Integer energyScore,
        Instant occurredAt,
        @NotBlank @Size(max = 64) String timezoneAtEntry,
        Boolean favorite) {}
