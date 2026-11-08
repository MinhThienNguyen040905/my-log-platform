package com.mylog.journal.api.request;

import com.mylog.journal.application.command.CompanionHistoryEntry;
import com.mylog.journal.application.command.CompanionTurnCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request for one companion turn. {@code text} alone (no {@code intent}) is the original
 * draft-screening contract and keeps working unchanged; {@code intent}/{@code message} is the
 * richer contract used by the chat panel. Exactly one of {@code OPEN}/{@code TOOL_COMPLETED}/
 * {@code CLOSING} needs no message; any other intent (including the legacy, intent-less call)
 * needs a non-blank {@code message} or {@code text}.
 */
public record WritingSuggestionRequest(@Size(max = 4000) String text,
                                       @Pattern(regexp = "OPEN|FREE_TEXT|TOOL_COMPLETED|CLOSING") String intent,
                                       @Size(max = 300) String message,
                                       @Valid CompanionDraftRequest draft,
                                       @Valid CompanionContextRequest context,
                                       @Size(max = 32) String completedTool,
                                       @Size(max = 8) @Valid List<CompanionHistoryTurnRequest> history) {
    @AssertTrue(message = "message hoặc text không được để trống")
    public boolean isContentPresent() {
        String effective = effectiveIntent();
        if ("OPEN".equals(effective) || "TOOL_COMPLETED".equals(effective) || "CLOSING".equals(effective)) {
            return true;
        }
        return hasText(message) || hasText(text);
    }

    public String effectiveIntent() {
        return (intent == null || intent.isBlank()) ? "FREE_TEXT" : intent;
    }

    public CompanionTurnCommand command() {
        List<CompanionHistoryEntry> entries = history == null ? List.of()
                : history.stream().map(h -> new CompanionHistoryEntry(h.role(), h.text())).toList();
        return new CompanionTurnCommand(effectiveIntent(), hasText(message) ? message : text,
                draft == null ? null : draft.title(), draft == null ? null : draft.text(),
                context == null ? null : context.moodCode(), context == null ? null : context.stressScore(),
                context == null ? null : context.energyScore(), context == null ? null : context.sleepMinutes(),
                context == null ? null : context.localHour(), completedTool, entries);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
