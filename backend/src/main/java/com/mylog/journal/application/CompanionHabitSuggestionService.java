package com.mylog.journal.application;

import com.mylog.journal.application.command.CompanionTurnCommand;
import com.mylog.journal.application.result.CompanionHabitSuggestion;
import com.mylog.journal.domain.HabitTemplateCatalog;
import com.mylog.selfcare.application.SelfCareCompanionQuery;
import com.mylog.selfcare.application.query.CompanionHabitCandidate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Companion-owned rules for attaching a self-care habit suggestion to a turn:
 * <ul>
 *   <li>Never when mood is {@code very_bad}: the user is not handed another thing to do.</li>
 *   <li>{@code OPEN}/{@code FREE_TEXT}: only when the generative companion chose a reviewed template
 *       ({@code requestedTemplate}); an existing active habit with the same tool, not yet done today, is
 *       offered for logging instead of creating a duplicate.</li>
 *   <li>{@code TOOL_COMPLETED}/{@code CLOSING}: priority 1 active habit matching {@code completedTool} not yet
 *       completed today; priority 2 (only when priority 1 misses) active habit with positive mood correlation;
 *       priority 3 reviewed template from {@code HabitTemplateCatalog}.</li>
 * </ul>
 * There is no daily cap or dismissal window: "Không, cảm ơn" only hides the card in the current conversation.
 */
@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class CompanionHabitSuggestionService {
    private final SelfCareCompanionQuery selfCareQuery;

    public CompanionHabitSuggestionService(SelfCareCompanionQuery selfCareQuery) {
        this.selfCareQuery = selfCareQuery;
    }

    @Transactional(readOnly = true)
    public Optional<CompanionHabitSuggestion> suggestForTurn(UUID userId, CompanionTurnCommand command,
                                                             String requestedTemplate) {
        if (command == null) return Optional.empty();
        if ("very_bad".equalsIgnoreCase(command.moodCode())) return Optional.empty();
        String intent = command.intent();
        if ("TOOL_COMPLETED".equals(intent) || "CLOSING".equals(intent)) {
            return Optional.of(afterToolOrClosing(userId, command));
        }
        return HabitTemplateCatalog.byCode(requestedTemplate).map(template -> fromTemplate(userId, template));
    }

    private CompanionHabitSuggestion fromTemplate(UUID userId, HabitTemplateCatalog.Template template) {
        if (template.presetToolCode() != null) {
            var match = selfCareQuery.findUncompletedActiveHabitByToolCode(userId, template.presetToolCode());
            if (match.isPresent()) return logExisting(match.get());
        }
        return CompanionHabitSuggestion.fromTemplate(template);
    }

    private CompanionHabitSuggestion afterToolOrClosing(UUID userId, CompanionTurnCommand command) {
        if (command.completedTool() != null && !command.completedTool().isBlank()) {
            var match = selfCareQuery.findUncompletedActiveHabitByToolCode(userId, command.completedTool());
            if (match.isPresent()) return logExisting(match.get());
        }

        var correlated = selfCareQuery.findUncompletedPositiveMoodAssociatedHabit(userId);
        if (correlated.isPresent()) {
            var c = correlated.get();
            return CompanionHabitSuggestion.logExisting(
                    c.habitId(), c.todayLocalDate(), c.targetValue(), c.unit(),
                    "Những ngày bạn duy trì “" + c.title() + "”, tâm trạng thường nhẹ hơn. Ghi nhận hôm nay nhé?");
        }

        return CompanionHabitSuggestion.fromTemplate(
                HabitTemplateCatalog.forContext(command.completedTool(), command.moodCode()));
    }

    private static CompanionHabitSuggestion logExisting(CompanionHabitCandidate c) {
        return CompanionHabitSuggestion.logExisting(
                c.habitId(), c.todayLocalDate(), c.targetValue(), c.unit(),
                "Ghi nhận thói quen “" + c.title() + "” hôm nay nhé?");
    }
}
