package com.mylog.journal.api;

import com.mylog.journal.domain.ContentFormat;
import com.mylog.journal.domain.JournalEntry;
import com.mylog.journal.domain.JournalStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record JournalResponse(
        UUID id,
        String title,
        String contentText,
        Map<String, Object> contentJson,
        ContentFormat contentFormat,
        int moodScore,
        Integer stressScore,
        Integer energyScore,
        JournalStatus status,
        long journalVersion,
        Instant occurredAt,
        LocalDate entryDate,
        String timezoneAtEntry,
        boolean favorite,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    public static JournalResponse from(JournalEntry entry) {
        return new JournalResponse(
                entry.getId(),
                entry.getTitle(),
                entry.getContentText(),
                entry.getContentJson(),
                entry.getContentFormat(),
                entry.getMoodScore(),
                entry.getStressScore() == null ? null : entry.getStressScore().intValue(),
                entry.getEnergyScore() == null ? null : entry.getEnergyScore().intValue(),
                entry.getStatus(),
                entry.getJournalVersion(),
                entry.getOccurredAt(),
                entry.getEntryDate(),
                entry.getTimezoneAtEntry(),
                entry.isFavorite(),
                entry.getCreatedAt(),
                entry.getUpdatedAt(),
                entry.getVersion());
    }
}
