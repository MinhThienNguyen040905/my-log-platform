package com.mylog.journal.api.response;

import com.mylog.journal.application.result.WritingSuggestionResult;

public record WritingSuggestionResponse(String status, String suggestion, String promptVersion) {
    public static WritingSuggestionResponse from(WritingSuggestionResult result) {
        return new WritingSuggestionResponse(result.status(), result.suggestion(), result.promptVersion());
    }
}
