package com.mylog.statistics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mylog.statistics.repository.StatisticsRepository;
import com.mylog.statistics.repository.StatisticsRepository.DailyMoodRow;
import com.mylog.statistics.service.StatisticsCache;
import com.mylog.statistics.service.StatisticsCalculator;
import com.mylog.statistics.service.StatisticsService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StatisticsServiceTest {

    @Test
    void cacheHitAvoidsDatabaseAndInvalidationChangesNamespace() {
        StatisticsRepository repository = repository();
        InMemoryCache cache = new InMemoryCache();
        StatisticsService service = service(repository, cache);
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 9, 22);

        service.mood(userId, date, date, "UTC");
        service.mood(userId, date, date, "UTC");
        verify(repository, times(2)).dailyMood(any(), any(), any(), anyString());

        service.invalidate(userId);
        service.mood(userId, date, date, "UTC");
        verify(repository, times(4)).dailyMood(any(), any(), any(), anyString());
    }

    @Test
    void unavailableCacheFallsBackToRepository() {
        StatisticsRepository repository = repository();
        StatisticsCache unavailable = new StatisticsCache() {
            public String version(UUID userId) { return "0"; }
            public <T> Optional<T> get(String key, Class<T> type) { return Optional.empty(); }
            public void put(String key, Object value, Duration ttl) {}
            public void invalidate(UUID userId) {}
        };

        assertThat(service(repository, unavailable)
                .mood(UUID.randomUUID(), LocalDate.of(2026, 9, 22), LocalDate.of(2026, 9, 22), "UTC")
                .days()).hasSize(1);
    }

    private StatisticsRepository repository() {
        StatisticsRepository repository = mock(StatisticsRepository.class);
        when(repository.dailyMood(any(), any(), any(), anyString())).thenReturn(java.util.List.of(
                new DailyMoodRow(LocalDate.of(2026, 9, 22), 1, new BigDecimal("7.00"), null, null)));
        when(repository.dayOfWeekMood(any(), any(), any(), anyString())).thenReturn(java.util.List.of());
        return repository;
    }

    private StatisticsService service(StatisticsRepository repository, StatisticsCache cache) {
        return new StatisticsService(
                repository,
                new StatisticsCalculator(),
                cache,
                Clock.fixed(Instant.parse("2026-09-22T00:00:00Z"), ZoneOffset.UTC));
    }

    private static final class InMemoryCache implements StatisticsCache {
        private final Map<String, Object> values = new HashMap<>();
        private long version;

        public String version(UUID userId) { return Long.toString(version); }

        public <T> Optional<T> get(String key, Class<T> type) {
            return Optional.ofNullable(values.get(key)).map(type::cast);
        }

        public void put(String key, Object value, Duration ttl) { values.put(key, value); }

        public void invalidate(UUID userId) { version++; }
    }
}
