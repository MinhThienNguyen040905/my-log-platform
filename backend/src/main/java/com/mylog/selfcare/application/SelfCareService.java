package com.mylog.selfcare.application;

import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.platform.id.IdGenerator;
import com.mylog.platform.web.ConflictException;
import com.mylog.platform.web.InvalidRequestException;
import com.mylog.platform.web.ResourceNotFoundException;
import com.mylog.selfcare.application.query.CompanionHabitCandidate;
import com.mylog.selfcare.application.query.GoalView;
import com.mylog.selfcare.application.query.HabitView;
import com.mylog.selfcare.application.query.HabitCompletionView;
import com.mylog.selfcare.application.query.MoodAssociationView;
import com.mylog.selfcare.domain.HabitSchedule;
import com.mylog.selfcare.domain.HabitMoodAssociation;
import com.mylog.checkin.application.CheckinService;
import com.mylog.user.application.UserProfileUseCase;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class SelfCareService implements SelfCareCompanionQuery {
    private static final Set<String> ALLOWED_TOOL_CODES = Set.of("BREATHING_2MIN", "AMBIENT", "VENT_PROMPT");
    private final SelfCareStore store;
    private final SensitiveDataCipher cipher;
    private final IdGenerator ids;
    private final UserProfileUseCase profiles;
    private final CheckinService checkins;
    private final Clock clock;

    public SelfCareService(SelfCareStore store, SensitiveDataCipher cipher, IdGenerator ids,
                           UserProfileUseCase profiles, CheckinService checkins, Clock clock) {
        this.store = store; this.cipher = cipher; this.ids = ids; this.profiles = profiles;
        this.checkins = checkins; this.clock = clock;
    }

    @Transactional
    public GoalView createGoal(UUID userId, String category, String title, String description,
                               LocalDate startDate, LocalDate targetDate) {
        if (category == null || !Set.of("SLEEP", "MINDFULNESS", "EXERCISE", "SOCIAL", "CUSTOM").contains(category)
                || invalidDateRange(startDate, targetDate)) throw new InvalidRequestException();
        String cleanTitle = wellnessText(title, 160, true);
        String cleanDescription = wellnessText(description, 2000, false);
        UUID id = ids.next(); Instant now = clock.instant();
        SelfCareStore.Goal goal = new SelfCareStore.Goal(id, userId, category,
                cipher.encrypt("selfcare-goal-title", userId, id, cleanTitle),
                cleanDescription == null ? null : cipher.encrypt("selfcare-goal-description", userId, id, cleanDescription),
                "ACTIVE", startDate, targetDate, now, now, null, 0);
        store.createGoal(goal);
        return view(goal);
    }

    @Transactional(readOnly = true)
    public List<GoalView> goals(UUID userId) { return store.goals(userId).stream().map(this::view).toList(); }

    @Transactional(readOnly = true)
    public List<HabitCompletionView> exportCompletions(UUID userId) {
        return store.exportAllCompletions(userId).stream().map(c -> new HabitCompletionView(c.id(),c.habitId(),
                c.localDate(),c.value(),c.source(),c.createdAt(),c.updatedAt())).toList();
    }

    @Transactional
    public GoalView updateGoal(UUID userId, UUID goalId, String title, String description,
                               String status, LocalDate startDate, LocalDate targetDate, long expectedVersion) {
        SelfCareStore.Goal old = requireGoal(userId, goalId);
        if (old.version() != expectedVersion) throw new ConflictException("Goal đã được cập nhật ở nơi khác.");
        if (status != null && !Set.of("ACTIVE", "PAUSED", "COMPLETED", "ARCHIVED").contains(status))
            throw new InvalidRequestException();
        LocalDate nextStart = startDate == null ? old.startDate() : startDate;
        LocalDate nextTarget = targetDate == null ? old.targetDate() : targetDate;
        if (invalidDateRange(nextStart, nextTarget)) throw new InvalidRequestException();
        Instant now = clock.instant();
        String nextStatus = status == null ? old.status() : status;
        SelfCareStore.Goal next = new SelfCareStore.Goal(old.id(), userId, old.category(),
                title == null ? old.title() : cipher.encrypt("selfcare-goal-title", userId, goalId,
                        wellnessText(title, 160, true)),
                description == null ? old.description() : cipher.encrypt("selfcare-goal-description", userId, goalId,
                        wellnessText(description, 2000, false)), nextStatus, nextStart, nextTarget,
                old.createdAt(), now, "COMPLETED".equals(nextStatus)
                        ? old.completedAt() == null ? now : old.completedAt() : null, old.version() + 1);
        if (!store.updateGoal(next, expectedVersion)) throw new ConflictException("Goal đã được cập nhật ở nơi khác.");
        return view(next);
    }

    @Transactional
    public HabitView createHabit(UUID userId, UUID goalId, String title, BigDecimal targetValue,
                                 String unit, String frequencyType, List<Integer> daysOfWeek) {
        return createHabit(userId, goalId, title, targetValue, unit, frequencyType, daysOfWeek, null);
    }

    @Transactional
    public HabitView createHabit(UUID userId, UUID goalId, String title, BigDecimal targetValue,
                                 String unit, String frequencyType, List<Integer> daysOfWeek, String toolCode) {
        SelfCareStore.Goal goal = requireGoal(userId, goalId);
        if (!"ACTIVE".equals(goal.status())) throw new ConflictException("Goal không còn active.");
        String cleanTitle = wellnessText(title, 160, true);
        String cleanToolCode = normalizeToolCode(toolCode);
        if (targetValue == null || targetValue.signum() <= 0 || targetValue.scale() > 2
                || targetValue.precision() > 10 || unit == null || !unit.matches("[A-Za-z0-9_]{1,32}"))
            throw new InvalidRequestException();
        HabitSchedule schedule = schedule(frequencyType, daysOfWeek);
        String timezone = profiles.get(userId).timezone();
        UUID id = ids.next(); Instant now = clock.instant();
        SelfCareStore.Habit habit = new SelfCareStore.Habit(id, goalId, userId,
                cipher.encrypt("selfcare-habit-title", userId, id, cleanTitle), targetValue, unit,
                schedule.type(), Map.of("daysOfWeek", schedule.daysOfWeek().stream().sorted().toList()),
                timezone, cleanToolCode, "ACTIVE", now, now, 0);
        store.createHabit(habit);
        return view(habit);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CompanionHabitCandidate> findUncompletedActiveHabitByToolCode(UUID userId, String toolCode) {
        if (toolCode == null || !ALLOWED_TOOL_CODES.contains(toolCode)) return Optional.empty();
        for (SelfCareStore.Goal goal : store.goals(userId)) {
            if (!"ACTIVE".equals(goal.status())) continue;
            for (SelfCareStore.Habit habit : store.habits(userId, goal.id())) {
                if (!"ACTIVE".equals(habit.status()) || !toolCode.equals(habit.toolCode())) continue;
                LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(habit.timezone())));
                if (!schedule(habit).scheduled(today) || isCompletedOn(userId, habit, today)) continue;
                String title = cipher.decrypt("selfcare-habit-title", habit.userId(), habit.id(), habit.title());
                return Optional.of(new CompanionHabitCandidate(
                        habit.id(), title, habit.targetValue(), habit.unit(), habit.toolCode(), today, null));
            }
        }
        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CompanionHabitCandidate> findUncompletedPositiveMoodAssociatedHabit(UUID userId) {
        CompanionHabitCandidate best = null;
        for (SelfCareStore.Goal goal : store.goals(userId)) {
            if (!"ACTIVE".equals(goal.status())) continue;
            for (SelfCareStore.Habit habit : store.habits(userId, goal.id())) {
                if (!"ACTIVE".equals(habit.status())) continue;
                LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(habit.timezone())));
                HabitSchedule schedule = schedule(habit);
                if (!schedule.scheduled(today)) continue;
                LocalDate from = habit.createdAt().atZone(ZoneId.of(habit.timezone())).toLocalDate();
                LocalDate sampleStart = today.minusDays(89).isAfter(from) ? today.minusDays(89) : from;
                List<SelfCareStore.Completion> completions = store.completions(userId, habit.id(), sampleStart, today);
                Set<LocalDate> achieved = completions.stream()
                        .filter(c -> c.value().compareTo(habit.targetValue()) >= 0)
                        .map(SelfCareStore.Completion::localDate)
                        .collect(Collectors.toSet());
                if (achieved.contains(today)) continue;
                var moodDays = checkins.list(userId, sampleStart, today).stream()
                        .map(c -> new HabitMoodAssociation.Day(c.localDate(), c.moodScore())).toList();
                var association = HabitMoodAssociation.calculate(schedule, achieved, moodDays);
                if (association == null || association.averageMoodDifference().signum() <= 0) continue;
                if (best == null || association.averageMoodDifference().compareTo(best.moodDifference()) > 0) {
                    String title = cipher.decrypt("selfcare-habit-title", habit.userId(), habit.id(), habit.title());
                    best = new CompanionHabitCandidate(habit.id(), title, habit.targetValue(), habit.unit(),
                            habit.toolCode(), today, association.averageMoodDifference());
                }
            }
        }
        return Optional.ofNullable(best);
    }

    private boolean isCompletedOn(UUID userId, SelfCareStore.Habit habit, LocalDate date) {
        return store.completions(userId, habit.id(), date, date).stream()
                .anyMatch(c -> c.value().compareTo(habit.targetValue()) >= 0);
    }

    private static String normalizeToolCode(String toolCode) {
        if (toolCode == null || toolCode.isBlank()) return null;
        String trimmed = toolCode.trim();
        if (!ALLOWED_TOOL_CODES.contains(trimmed)) throw new InvalidRequestException("Mã công cụ không hợp lệ.");
        return trimmed;
    }

    @Transactional
    public HabitView putCompletion(UUID userId, UUID habitId, LocalDate date, BigDecimal value) {
        SelfCareStore.Habit habit = requireHabit(userId, habitId);
        if (!"ACTIVE".equals(habit.status()) || !"ACTIVE".equals(requireGoal(userId, habit.goalId()).status()))
            throw new ConflictException("Habit hoặc goal không còn active.");
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(habit.timezone())));
        if (date == null || date.isAfter(today) || date.isBefore(habit.createdAt().atZone(ZoneId.of(habit.timezone())).toLocalDate())
                || value == null || value.signum() <= 0 || value.scale() > 2 || value.precision() > 10
                || !schedule(habit).scheduled(date)) throw new InvalidRequestException();
        Instant now = clock.instant();
        store.putCompletion(new SelfCareStore.Completion(ids.next(), habitId, userId, date, value, "USER", now, now));
        return view(habit);
    }

    @Transactional
    public void deleteCompletion(UUID userId, UUID habitId, LocalDate date) {
        requireHabit(userId, habitId);
        store.deleteCompletion(userId, habitId, date);
    }

    private GoalView view(SelfCareStore.Goal goal) {
        return new GoalView(goal.id(), goal.category(), cipher.decrypt("selfcare-goal-title", goal.userId(), goal.id(), goal.title()),
                goal.description() == null ? null : cipher.decrypt("selfcare-goal-description", goal.userId(), goal.id(), goal.description()),
                goal.status(), goal.startDate(), goal.targetDate(), goal.version(), goal.createdAt(), goal.updatedAt(),
                store.habits(goal.userId(), goal.id()).stream().map(this::view).toList());
    }

    private HabitView view(SelfCareStore.Habit habit) {
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(habit.timezone())));
        LocalDate from = habit.createdAt().atZone(ZoneId.of(habit.timezone())).toLocalDate();
        List<SelfCareStore.Completion> completions = store.completions(habit.userId(), habit.id(), from, today);
        List<LocalDate> achieved = completions.stream().filter(c -> c.value().compareTo(habit.targetValue()) >= 0)
                .map(SelfCareStore.Completion::localDate).toList();
        HabitSchedule schedule = schedule(habit);
        LocalDate sampleStart = today.minusDays(89).isAfter(from) ? today.minusDays(89) : from;
        var moodDays = checkins.list(habit.userId(), sampleStart, today).stream()
                .map(c -> new HabitMoodAssociation.Day(c.localDate(), c.moodScore())).toList();
        var association = HabitMoodAssociation.calculate(schedule, Set.copyOf(achieved), moodDays);
        var moodAssociation = association == null ? null : new MoodAssociationView(association.completedDays(),
                association.otherDays(), association.averageMoodDifference(),
                "Chỉ là mối liên hệ quan sát được, không chứng minh nguyên nhân.");
        return new HabitView(habit.id(), habit.goalId(), cipher.decrypt("selfcare-habit-title", habit.userId(), habit.id(), habit.title()),
                habit.targetValue(), habit.unit(), habit.frequencyType(), schedule.daysOfWeek().stream().sorted().toList(),
                habit.timezone(), habit.toolCode(), habit.status(), habit.version(), schedule.streak(today, achieved),
                achieved.size(), achieved, moodAssociation);
    }

    private SelfCareStore.Goal requireGoal(UUID userId, UUID id) {
        return store.goal(userId, id).orElseThrow(() -> new ResourceNotFoundException("Goal không tồn tại."));
    }
    private SelfCareStore.Habit requireHabit(UUID userId, UUID id) {
        return store.habit(userId, id).orElseThrow(() -> new ResourceNotFoundException("Habit không tồn tại."));
    }
    private static HabitSchedule schedule(String type, List<Integer> days) {
        try {
            if (days != null && days.stream().anyMatch(d -> d == null)) throw new InvalidRequestException();
            return new HabitSchedule(type, days == null ? Set.of() : Set.copyOf(days));
        } catch (IllegalArgumentException e) { throw new InvalidRequestException(); }
    }
    @SuppressWarnings("unchecked")
    private static HabitSchedule schedule(SelfCareStore.Habit habit) {
        List<Integer> days = ((List<?>) habit.frequencyConfig().getOrDefault("daysOfWeek", List.of()))
                .stream().map(x -> ((Number) x).intValue()).collect(Collectors.toList());
        return schedule(habit.frequencyType(), days);
    }
    private static boolean invalidDateRange(LocalDate start, LocalDate target) {
        return start != null && target != null && target.isBefore(start);
    }
    private static String wellnessText(String value, int max, boolean required) {
        if (value == null) {
            if (required) throw new InvalidRequestException();
            return null;
        }
        String text = value.trim();
        if (text.isEmpty() || text.length() > max || text.chars().anyMatch(Character::isISOControl))
            throw new InvalidRequestException();
        String normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT).replace('đ', 'd');
        if (normalized.matches(".*(chan doan|dieu tri|ke don|thuoc dieu tri|lieu trinh|tri lieu|diagnos|prescri|treatment plan|medication plan).*"))
            throw new InvalidRequestException();
        return text;
    }
}
