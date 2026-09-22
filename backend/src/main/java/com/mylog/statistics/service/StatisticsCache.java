package com.mylog.statistics.service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface StatisticsCache {

    String version(UUID userId);

    <T> Optional<T> get(String key, Class<T> type);

    void put(String key, Object value, Duration ttl);

    void invalidate(UUID userId);
}
