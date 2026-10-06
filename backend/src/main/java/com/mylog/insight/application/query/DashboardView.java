package com.mylog.insight.application.query;

import java.time.LocalDate;
import java.util.List;

public record DashboardView(String range, String timezone, LocalDate from, LocalDate to,
                            List<DailyMetric> timeline, List<TopCode> topEmotions,
                            List<TopCode> topTopics, int currentJournalStreak,
                            int longestJournalStreak, int journalEntryCount, int moodSampleSize) {}
