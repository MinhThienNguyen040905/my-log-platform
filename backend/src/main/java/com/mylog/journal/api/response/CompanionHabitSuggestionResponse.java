package com.mylog.journal.api.response;

import com.mylog.journal.application.result.CompanionHabitSuggestion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CompanionHabitSuggestionResponse(String type, UUID habitId, LocalDate localDate,
                                               BigDecimal targetValue, String unit,
                                               String templateCode, String category, String presetTitle,
                                               BigDecimal presetTargetValue, String presetUnit,
                                               String presetToolCode, String text) {
    public static CompanionHabitSuggestionResponse from(CompanionHabitSuggestion suggestion) {
        if (suggestion == null) return null;
        return new CompanionHabitSuggestionResponse(
                suggestion.type(), suggestion.habitId(), suggestion.localDate(),
                suggestion.targetValue(), suggestion.unit(), suggestion.templateCode(),
                suggestion.category(), suggestion.presetTitle(), suggestion.presetTargetValue(),
                suggestion.presetUnit(), suggestion.presetToolCode(), suggestion.text());
    }
}
