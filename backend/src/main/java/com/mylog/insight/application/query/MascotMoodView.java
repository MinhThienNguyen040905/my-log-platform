package com.mylog.insight.application.query;

import java.time.LocalDate;

/**
 * The mood the Insight-page mascot shows: the mood icon picked most often on journal entries in the last 7 local
 * days ({@code mood} null when there are none). Ties go to the mood of the most recent entry. {@code calm} is true
 * when an entry in the window was screened HIGH/CRITICAL, so the client keeps the mascot still and gentle.
 */
public record MascotMoodView(String mood, int moodCount, int entryCount, LocalDate from, LocalDate to, boolean calm) {}
