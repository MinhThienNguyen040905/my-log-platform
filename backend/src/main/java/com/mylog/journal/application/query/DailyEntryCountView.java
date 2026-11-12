package com.mylog.journal.application.query;

import java.time.LocalDate;

/**
 * Saved entries on one local day, for the calendar. {@code latestMoodCode} is the mood icon of the day's most
 * recent entry (may be null). No journal content is read or decrypted to build this.
 */
public record DailyEntryCountView(LocalDate date, int count, String latestMoodCode) {}
