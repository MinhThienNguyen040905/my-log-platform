package com.mylog.insight.application;

import com.mylog.insight.application.query.ActivitySignal;
import com.mylog.insight.application.query.DailyMetric;
import com.mylog.insight.application.query.EntrySignal;
import com.mylog.insight.application.query.TopCode;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DailyAggregateSource {
    String timezone(UUID userId);
    List<DailyMetric> daily(UUID userId, LocalDate from, LocalDate to);
    int currentJournalStreak(UUID userId, LocalDate today);
    /** Primary (rank 1) emotion of each current analysis: one per analyzed entry. */
    List<TopCode> topEmotions(UUID userId, LocalDate from, LocalDate to, int limit);
    /** Mood icons the user picked on saved journal entries ({@code mood_code}), one per entry. */
    List<TopCode> topMoods(UUID userId, LocalDate from, LocalDate to, int limit);
    List<TopCode> topTopics(UUID userId, LocalDate from, LocalDate to, int limit);
    /** Saved entries in the window with scores, current analysis codes (if ANALYZED) and tag IDs. */
    List<EntrySignal> entrySignals(UUID userId, LocalDate from, LocalDate to);
    List<ActivitySignal> activities(UUID userId, LocalDate from, LocalDate to);
    /** Most picked mood icon in the window (ties: the most recent entry's), or null; plus the entry count. */
    record DominantMood(String code, int count, int entries) {}
    DominantMood dominantMood(UUID userId, LocalDate from, LocalDate to);
    /** True when any saved entry in the window was screened HIGH or CRITICAL. */
    boolean hasElevatedRisk(UUID userId, LocalDate from, LocalDate to);
}
