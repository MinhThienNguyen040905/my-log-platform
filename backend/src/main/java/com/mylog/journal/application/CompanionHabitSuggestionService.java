package com.mylog.journal.application;

import com.mylog.journal.application.command.CompanionTurnCommand;
import com.mylog.journal.application.result.CompanionHabitSuggestion;
import com.mylog.journal.domain.HabitTemplateCatalog;
import com.mylog.selfcare.application.SelfCareCompanionQuery;
import com.mylog.user.application.UserProfileUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Companion-owned rules for suggesting a self-care habit on {@code TOOL_COMPLETED} or {@code CLOSING}
 * turns (see COMPANION_CHATBOT_PLAN.md stage 3 & 5):
 * <ul>
 *   <li>Never on {@code OPEN} or {@code FREE_TEXT}; never when mood is {@code very_bad}.</li>
 *   <li>At most 1 suggestion per calendar day; hidden for 7 days after user clicks "Không, cảm ơn".</li>
 *   <li>Priority 1: active habit matching {@code completedTool} not yet completed today.</li>
 *   <li>Priority 2 (only evaluated when Priority 1 misses): active habit with positive mood correlation.</li>
 *   <li>Priority 3: reviewed template from {@code HabitTemplateCatalog}.</li>
 * </ul>
 */
@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class CompanionHabitSuggestionService {
    private final SelfCareCompanionQuery selfCareQuery;
    private final CompanionHabitPreferenceStore preferenceStore;
    private final UserProfileUseCase userProfiles;
    private final Clock clock;

    public CompanionHabitSuggestionService(SelfCareCompanionQuery selfCareQuery,
                                           CompanionHabitPreferenceStore preferenceStore,
                                           UserProfileUseCase userProfiles,
                                           Clock clock) {
        this.selfCareQuery = selfCareQuery;
        this.preferenceStore = preferenceStore;
        this.userProfiles = userProfiles;
        this.clock = clock;
    }

    @Transactional
    public Optional<CompanionHabitSuggestion> suggestForTurn(UUID userId, CompanionTurnCommand command) {
        if (command == null) return Optional.empty();
        String intent = command.intent();
        if (!("TOOL_COMPLETED".equals(intent) || "CLOSING".equals(intent))) {
            return Optional.empty();
        }
        if ("very_bad".equalsIgnoreCase(command.moodCode())) {
            return Optional.empty();
        }

        Instant now = clock.instant();
        LocalDate today = userLocalDate(userId);
        var prefOpt = preferenceStore.find(userId);
        if (prefOpt.isPresent()) {
            var pref = prefOpt.get();
            if (pref.dismissedUntil() != null && pref.dismissedUntil().isAfter(now)) {
                return Optional.empty();
            }
            if (today.equals(pref.lastSuggestedDate())) {
                return Optional.empty();
            }
        }

        if (command.completedTool() != null && !command.completedTool().isBlank()) {
            var match = selfCareQuery.findUncompletedActiveHabitByToolCode(userId, command.completedTool());
            if (match.isPresent()) {
                var c = match.get();
                preferenceStore.recordSuggestedOn(userId, today, now);
                return Optional.of(CompanionHabitSuggestion.logExisting(
                        c.habitId(), c.todayLocalDate(), c.targetValue(), c.unit(),
                        "Ghi nhận thói quen “" + c.title() + "” hôm nay nhé?"));
            }
        }

        var correlated = selfCareQuery.findUncompletedPositiveMoodAssociatedHabit(userId);
        if (correlated.isPresent()) {
            var c = correlated.get();
            preferenceStore.recordSuggestedOn(userId, today, now);
            return Optional.of(CompanionHabitSuggestion.logExisting(
                    c.habitId(), c.todayLocalDate(), c.targetValue(), c.unit(),
                    "Những ngày bạn duy trì “" + c.title() + "”, tâm trạng thường nhẹ hơn. Ghi nhận hôm nay nhé?"));
        }

        var template = HabitTemplateCatalog.forContext(command.completedTool(), command.moodCode());
        preferenceStore.recordSuggestedOn(userId, today, now);
        return Optional.of(CompanionHabitSuggestion.fromTemplate(template));
    }

    @Transactional
    public void dismissForSevenDays(UUID userId) {
        Instant now = clock.instant();
        preferenceStore.dismissUntil(userId, now.plus(7, ChronoUnit.DAYS), now);
    }

    private LocalDate userLocalDate(UUID userId) {
        String tz = "UTC";
        try {
            var profile = userProfiles.get(userId);
            if (profile != null && profile.timezone() != null && !profile.timezone().isBlank()) {
                tz = profile.timezone();
            }
        } catch (Exception ignored) {}
        return LocalDate.now(clock.withZone(ZoneId.of(tz)));
    }
}
