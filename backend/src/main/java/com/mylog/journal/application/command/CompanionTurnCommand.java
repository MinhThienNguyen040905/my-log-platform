package com.mylog.journal.application.command;

import java.math.BigDecimal;
import java.util.List;

/**
 * One turn sent to the writing companion. Built by the API layer from {@code WritingSuggestionRequest}.
 * {@code intent} is one of {@code OPEN}, {@code FREE_TEXT}, {@code TOOL_COMPLETED}, {@code CLOSING}.
 * Nothing in this command is logged or persisted; see {@code WritingSuggestionService}.
 */
public record CompanionTurnCommand(String intent, String message, String draftTitle, String draftText,
                                   String moodCode, BigDecimal stressScore, BigDecimal energyScore,
                                   Integer sleepMinutes, Integer localHour, String completedTool,
                                   List<CompanionHistoryEntry> history) {
    public CompanionTurnCommand {
        history = history == null ? List.of() : List.copyOf(history);
    }

    public boolean draftEmpty() {
        return draftText == null || draftText.isBlank();
    }

    /** Heuristic for the "already writing a lot" branch; not a hard limit, only a copy choice. */
    public boolean draftLong() {
        return draftText != null && draftText.trim().split("\\s+").length > 150;
    }
}
