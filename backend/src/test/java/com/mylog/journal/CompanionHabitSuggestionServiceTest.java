package com.mylog.journal;

import com.mylog.journal.application.CompanionHabitPreferenceStore;
import com.mylog.journal.application.CompanionHabitSuggestionService;
import com.mylog.journal.application.command.CompanionTurnCommand;
import com.mylog.selfcare.application.SelfCareCompanionQuery;
import com.mylog.selfcare.application.query.CompanionHabitCandidate;
import com.mylog.user.application.UserProfileUseCase;
import com.mylog.user.application.query.ConsentView;
import com.mylog.user.application.query.ProfileView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CompanionHabitSuggestionServiceTest {
    private final UUID userId = UUID.randomUUID();
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-08T10:00:00Z"), ZoneId.of("UTC"));

    private Optional<CompanionHabitCandidate> tier1Result = Optional.empty();
    private Optional<CompanionHabitCandidate> tier2Result = Optional.empty();
    private final AtomicInteger tier1Calls = new AtomicInteger();
    private final AtomicInteger tier2Calls = new AtomicInteger();

    private Optional<CompanionHabitPreferenceStore.Preference> currentPref = Optional.empty();
    private LocalDate lastRecordedSuggestedDate;
    private Instant lastRecordedDismissedUntil;

    private CompanionHabitSuggestionService service;

    @BeforeEach
    void setUp() {
        SelfCareCompanionQuery selfCareQuery = new SelfCareCompanionQuery() {
            @Override
            public Optional<CompanionHabitCandidate> findUncompletedActiveHabitByToolCode(UUID uid, String toolCode) {
                tier1Calls.incrementAndGet();
                return tier1Result;
            }

            @Override
            public Optional<CompanionHabitCandidate> findUncompletedPositiveMoodAssociatedHabit(UUID uid) {
                tier2Calls.incrementAndGet();
                return tier2Result;
            }
        };

        CompanionHabitPreferenceStore preferenceStore = new CompanionHabitPreferenceStore() {
            @Override
            public Optional<Preference> find(UUID uid) {
                return currentPref;
            }

            @Override
            public void recordSuggestedOn(UUID uid, LocalDate localDate, Instant now) {
                lastRecordedSuggestedDate = localDate;
            }

            @Override
            public void dismissUntil(UUID uid, Instant dismissedUntil, Instant now) {
                lastRecordedDismissedUntil = dismissedUntil;
            }
        };

        UserProfileUseCase userProfiles = new StubUserProfileUseCase();
        service = new CompanionHabitSuggestionService(selfCareQuery, preferenceStore, userProfiles, clock);
    }

    @Test
    void neverSuggestsOnOpenOrFreeTextOrVeryBadMood() {
        assertTrue(service.suggestForTurn(userId, turn("OPEN", "sad", "BREATHING_2MIN")).isEmpty());
        assertTrue(service.suggestForTurn(userId, turn("FREE_TEXT", "sad", "BREATHING_2MIN")).isEmpty());
        assertTrue(service.suggestForTurn(userId, turn("TOOL_COMPLETED", "very_bad", "BREATHING_2MIN")).isEmpty());
        assertTrue(service.suggestForTurn(userId, turn("CLOSING", "very_bad", null)).isEmpty());
        assertEquals(0, tier1Calls.get());
        assertEquals(0, tier2Calls.get());
    }

    @Test
    void priority1MatchesToolCodeAndSkipsMoodCorrelationCalculation() {
        UUID habitId = UUID.randomUUID();
        LocalDate habitToday = LocalDate.of(2026, 11, 8);
        tier1Result = Optional.of(new CompanionHabitCandidate(
                habitId, "Thở sâu mỗi tối", BigDecimal.valueOf(2), "minutes",
                "BREATHING_2MIN", habitToday, null));

        var result = service.suggestForTurn(userId, turn("TOOL_COMPLETED", "sad", "BREATHING_2MIN"));
        assertTrue(result.isPresent());
        assertEquals("LOG_EXISTING", result.get().type());
        assertEquals(habitId, result.get().habitId());
        assertEquals(habitToday, result.get().localDate());
        assertTrue(result.get().text().contains("Thở sâu mỗi tối"));

        // Point 9: Priority 2 (mood correlation) must NOT be called when Priority 1 matches
        assertEquals(1, tier1Calls.get());
        assertEquals(0, tier2Calls.get());
        assertEquals(habitToday, lastRecordedSuggestedDate);
    }

    @Test
    void priority2UsesNonCausalCorrelationCopyWhenToolCodeMisses() {
        UUID habitId = UUID.randomUUID();
        LocalDate habitToday = LocalDate.of(2026, 11, 8);
        tier1Result = Optional.empty();
        tier2Result = Optional.of(new CompanionHabitCandidate(
                habitId, "Đi bộ chiều", BigDecimal.valueOf(15), "minutes",
                null, habitToday, new BigDecimal("0.85")));

        var result = service.suggestForTurn(userId, turn("TOOL_COMPLETED", "sad", "BREATHING_2MIN"));
        assertTrue(result.isPresent());
        assertEquals("LOG_EXISTING", result.get().type());
        assertEquals(habitId, result.get().habitId());
        assertTrue(result.get().text().contains("tâm trạng thường nhẹ hơn"));
        assertEquals(1, tier2Calls.get());
    }

    @Test
    void priority3FallsBackToReviewedTemplate() {
        tier1Result = Optional.empty();
        tier2Result = Optional.empty();

        var result = service.suggestForTurn(userId, turn("TOOL_COMPLETED", "angry", "BREATHING_2MIN"));
        assertTrue(result.isPresent());
        assertEquals("CREATE_FROM_TEMPLATE", result.get().type());
        assertEquals("EVENING_BREATHING", result.get().templateCode());
        assertEquals("BREATHING_2MIN", result.get().presetToolCode());
    }

    @Test
    void enforcesOncePerDayAndSevenDayDismissal() {
        LocalDate today = LocalDate.of(2026, 11, 8);
        currentPref = Optional.of(
                new CompanionHabitPreferenceStore.Preference(userId, today, null, clock.instant()));
        assertTrue(service.suggestForTurn(userId, turn("CLOSING", "sad", null)).isEmpty());

        currentPref = Optional.of(
                new CompanionHabitPreferenceStore.Preference(userId, null,
                        clock.instant().plusSeconds(3600), clock.instant()));
        assertTrue(service.suggestForTurn(userId, turn("CLOSING", "sad", null)).isEmpty());

        service.dismissForSevenDays(userId);
        assertEquals(Instant.parse("2026-11-15T10:00:00Z"), lastRecordedDismissedUntil);
    }

    private static CompanionTurnCommand turn(String intent, String moodCode, String completedTool) {
        return new CompanionTurnCommand(intent, null, "Tiêu đề", "Nội dung nhật ký",
                moodCode, null, null, null, 21, completedTool, List.of());
    }

    static final class StubUserProfileUseCase implements UserProfileUseCase {
        @Override public void validateRegistrationPreferences(String timezone, String locale) {}
        @Override public void createDefault(UUID userId, String timezone, String locale, String termsVersion, String privacyVersion, Instant now) {}
        @Override public ProfileView get(UUID userId) {
            return new ProfileView(userId, "User", null, List.of(), "Asia/Ho_Chi_Minh", "vi", null, 0L);
        }
        @Override public ProfileView update(UUID userId, String displayName, String penName, List<String> onboardingGoals, String timezone, String locale, long expectedVersion) { return null; }
        @Override public ProfileView completeOnboarding(UUID userId, long expectedVersion) { return null; }
        @Override public void decideConsent(UUID userId, String type, String documentVersion, boolean granted) {}
        @Override public List<ConsentView> consents(UUID userId) { return List.of(); }
        @Override public boolean isConsentGranted(UUID userId, String type) { return true; }
    }
}
