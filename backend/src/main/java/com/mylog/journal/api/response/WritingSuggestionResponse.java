package com.mylog.journal.api.response;

import com.mylog.journal.application.result.CompanionReply;
import com.mylog.journal.application.result.CompanionTurnResult;

import java.util.List;

/**
 * {@code status} is one of {@code REPLY}, {@code CONSENT_REQUIRED}, {@code SAFETY_FLOW},
 * {@code BLOCKED}, {@code UNAVAILABLE}. Every other field is null unless {@code status} is
 * {@code REPLY} (all fields may be populated) or {@code BLOCKED} (only {@code reply}, no
 * questions or actions). {@code habitSuggestion} is reserved for a later stage and always null today.
 */
public record WritingSuggestionResponse(String status, String reply, List<String> questions,
                                        String suggestedTitle, List<CompanionActionResponse> immediateActions,
                                        CompanionHabitSuggestionResponse habitSuggestion,
                                        List<CompanionChipResponse> chips, String nextState,
                                        String source, String promptVersion) {
    public static WritingSuggestionResponse from(CompanionTurnResult result) {
        CompanionReply body = result.reply();
        if (body == null) {
            return new WritingSuggestionResponse(result.status(), null, null, null, null, null, null, null, null, null);
        }
        return new WritingSuggestionResponse(result.status(), body.reply(), body.questions(), body.suggestedTitle(),
                body.immediateActions().stream().map(CompanionActionResponse::from).toList(),
                CompanionHabitSuggestionResponse.from(body.habitSuggestion()),
                body.chips().stream().map(CompanionChipResponse::from).toList(),
                body.nextState(), body.source(), body.promptVersion());
    }
}
