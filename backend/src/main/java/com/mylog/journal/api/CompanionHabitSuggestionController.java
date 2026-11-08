package com.mylog.journal.api;

import com.mylog.journal.application.CompanionHabitSuggestionService;
import com.mylog.platform.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dedicated endpoint for dismissing companion habit suggestions for 7 days.
 * Kept separate from {@code /api/v1/journal-writing-suggestions} so clicking "Không, cảm ơn"
 * does not consume a {@code companion-turn} quota slot or run AI consent/safety gates.
 */
@RestController
@RequestMapping("/api/v1/companion/habit-suggestions")
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class CompanionHabitSuggestionController {
    private final CurrentUserProvider current;
    private final CompanionHabitSuggestionService habitSuggestions;

    public CompanionHabitSuggestionController(CurrentUserProvider current,
                                              CompanionHabitSuggestionService habitSuggestions) {
        this.current = current;
        this.habitSuggestions = habitSuggestions;
    }

    @PostMapping("/dismiss")
    public ResponseEntity<Void> dismiss() {
        habitSuggestions.dismissForSevenDays(current.requireCurrent().userId());
        return ResponseEntity.noContent().build();
    }
}
