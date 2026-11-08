package com.mylog.journal.application.result;

import com.mylog.journal.domain.HabitTemplateCatalog.Template;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Optional habit suggestion attached to a {@code TOOL_COMPLETED} or {@code CLOSING} companion turn.
 * For {@code LOG_EXISTING}, {@code localDate} is computed in the habit's own timezone.
 */
public record CompanionHabitSuggestion(String type, UUID habitId, LocalDate localDate,
                                       BigDecimal targetValue, String unit,
                                       String templateCode, String category, String presetTitle,
                                       BigDecimal presetTargetValue, String presetUnit,
                                       String presetToolCode, String text) {
    public static CompanionHabitSuggestion logExisting(UUID habitId, LocalDate localDate,
                                                       BigDecimal targetValue, String unit, String text) {
        return new CompanionHabitSuggestion("LOG_EXISTING", habitId, localDate, targetValue, unit,
                null, null, null, null, null, null, text);
    }

    public static CompanionHabitSuggestion fromTemplate(Template template) {
        return new CompanionHabitSuggestion("CREATE_FROM_TEMPLATE", null, null, null, null,
                template.templateCode(), template.category(), template.presetTitle(),
                template.presetTargetValue(), template.presetUnit(), template.presetToolCode(),
                template.text());
    }
}
