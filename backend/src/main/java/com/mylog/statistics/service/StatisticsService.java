package com.mylog.statistics.service;

import com.mylog.common.api.ApiErrorCodes;
import com.mylog.common.exception.BadRequestException;
import com.mylog.statistics.repository.StatisticsRepository;
import com.mylog.statistics.repository.StatisticsRepository.DailyMoodRow;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatisticsService {

    public static final String CALCULATION_VERSION = "statistics_v1";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final StatisticsRepository repository;
    private final StatisticsCalculator calculator;
    private final StatisticsCache cache;
    private final Clock clock;

    public StatisticsService(
            StatisticsRepository repository, StatisticsCalculator calculator,
            StatisticsCache cache, Clock clock) {
        this.repository = repository;
        this.calculator = calculator;
        this.cache = cache;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MoodStatisticsView mood(UUID userId, LocalDate from, LocalDate to, String timezone) {
        Range range = range(userId, from, to, timezone, 30);
        String key = key(userId, "mood", range);
        return cached(key, MoodStatisticsView.class, () -> calculateMood(userId, range));
    }

    @Transactional(readOnly = true)
    public EmotionStatisticsView emotions(UUID userId, LocalDate from, LocalDate to, String timezone) {
        Range range = range(userId, from, to, timezone, 30);
        String key = key(userId, "emotions", range);
        return cached(key, EmotionStatisticsView.class, () -> calculateEmotions(userId, range));
    }

    @Transactional(readOnly = true)
    public TopicStatisticsView topics(UUID userId, LocalDate from, LocalDate to, String timezone) {
        Range range = range(userId, from, to, timezone, 30);
        String key = key(userId, "topics", range);
        return cached(key, TopicStatisticsView.class, () -> calculateTopics(userId, range));
    }

    @Transactional(readOnly = true)
    public DashboardView dashboard(UUID userId, LocalDate from, LocalDate to, String timezone) {
        Range range = range(userId, from, to, timezone, 7);
        String key = key(userId, "dashboard", range);
        return cached(key, DashboardView.class, () -> new DashboardView(
                range.from(), range.to(), range.timezone(), CALCULATION_VERSION,
                calculateMood(userId, range), calculateEmotions(userId, range), calculateTopics(userId, range)));
    }

    public void invalidate(UUID userId) {
        cache.invalidate(userId);
    }

    private MoodStatisticsView calculateMood(UUID userId, Range range) {
        List<DailyMoodRow> currentRows = repository.dailyMood(
                userId, range.from(), range.to(), range.timezone());
        long days = ChronoUnit.DAYS.between(range.from(), range.to()) + 1;
        LocalDate previousTo = range.from().minusDays(1);
        LocalDate previousFrom = previousTo.minusDays(days - 1);
        BigDecimal current = calculator.weightedMoodAverage(currentRows);
        BigDecimal previous = calculator.weightedMoodAverage(
                repository.dailyMood(userId, previousFrom, previousTo, range.timezone()));
        return new MoodStatisticsView(
                range.from(), range.to(), range.timezone(), CALCULATION_VERSION,
                calculator.trend(current, previous), current, previous,
                currentRows.stream().map(row -> new MoodStatisticsView.DailyMood(
                        row.date(), row.journalCount(), row.moodAverage(),
                        row.stressAverage(), row.energyAverage())).toList(),
                repository.dayOfWeekMood(userId, range.from(), range.to(), range.timezone()).stream()
                        .map(row -> new MoodStatisticsView.DayOfWeekMood(
                                row.dayOfWeek(), row.sampleSize(), row.moodAverage()))
                        .toList());
    }

    private EmotionStatisticsView calculateEmotions(UUID userId, Range range) {
        var rows = repository.emotions(userId, range.from(), range.to(), range.timezone());
        BigDecimal total = rows.stream().map(row -> row.averageScore() == null ? BigDecimal.ZERO : row.averageScore())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new EmotionStatisticsView(
                range.from(), range.to(), range.timezone(), CALCULATION_VERSION,
                rows.stream().map(row -> new EmotionStatisticsView.Emotion(
                        row.type(), row.sampleSize(), row.averageScore(),
                        calculator.ratio(row.averageScore(), total))).toList());
    }

    private TopicStatisticsView calculateTopics(UUID userId, Range range) {
        int total = repository.journalCount(userId, range.from(), range.to(), range.timezone());
        return new TopicStatisticsView(
                range.from(), range.to(), range.timezone(), CALCULATION_VERSION, total,
                repository.topics(userId, range.from(), range.to(), range.timezone()).stream()
                        .map(row -> new TopicStatisticsView.Topic(
                                row.name(), row.journalCount(), calculator.ratio(row.journalCount(), total)))
                        .toList());
    }

    private <T> T cached(String key, Class<T> type, Supplier<T> loader) {
        return cache.get(key, type).orElseGet(() -> {
            T value = loader.get();
            cache.put(key, value, CACHE_TTL);
            return value;
        });
    }

    private String key(UUID userId, String type, Range range) {
        return userId + ":" + cache.version(userId) + ":" + CALCULATION_VERSION + ":" + type
                + ":" + range.from() + ":" + range.to() + ":" + range.timezone();
    }

    private Range range(UUID userId, LocalDate from, LocalDate to, String timezone, int defaultDays) {
        ZoneId zone;
        try {
            String resolvedTimezone = timezone == null || timezone.isBlank()
                    ? repository.findTimezone(userId)
                    : timezone;
            zone = ZoneId.of(resolvedTimezone);
        } catch (DateTimeException exception) {
            throw new BadRequestException(ApiErrorCodes.INVALID_REQUEST, "timezone is invalid");
        }
        LocalDate resolvedTo = to == null ? LocalDate.now(clock.withZone(zone)) : to;
        LocalDate resolvedFrom = from == null ? resolvedTo.minusDays(defaultDays - 1L) : from;
        long days = ChronoUnit.DAYS.between(resolvedFrom, resolvedTo) + 1;
        if (days < 1 || days > 366) {
            throw new BadRequestException(ApiErrorCodes.INVALID_REQUEST, "Date range must contain between 1 and 366 days");
        }
        return new Range(resolvedFrom, resolvedTo, zone.getId());
    }

    private record Range(LocalDate from, LocalDate to, String timezone) {}
}
