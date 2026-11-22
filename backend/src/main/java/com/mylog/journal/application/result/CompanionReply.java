package com.mylog.journal.application.result;

import com.mylog.journal.domain.CompanionActionCode;

import java.util.List;

/**
 * The content a {@code CompanionResponder} produces for an `ALLOW` turn.
 * <p>
 * {@code requestedHabitTemplate} is the habit template code the generative responder chose for this
 * turn (already checked against {@code HabitTemplateCatalog}), or null. It never reaches the API:
 * {@code CompanionHabitSuggestionService} turns it into {@code habitSuggestion}.
 */
public record CompanionReply(String reply, List<String> questions, String suggestedTitle,
                             List<CompanionActionCode> immediateActions,
                             CompanionHabitSuggestion habitSuggestion,
                             List<CompanionChip> chips,
                             String nextState, String source, String promptVersion,
                             String requestedHabitTemplate) {
    public CompanionReply(String reply, List<String> questions, String suggestedTitle,
                          List<CompanionActionCode> immediateActions, List<CompanionChip> chips,
                          String nextState, String source, String promptVersion) {
        this(reply, questions, suggestedTitle, immediateActions, null, chips, nextState, source, promptVersion, null);
    }

    public CompanionReply withHabitSuggestion(CompanionHabitSuggestion suggestion) {
        if (suggestion == null) return this;
        return new CompanionReply(reply, questions, suggestedTitle, immediateActions, suggestion, chips,
                nextState, source, promptVersion, requestedHabitTemplate);
    }
}
