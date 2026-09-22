package com.mylog.statistics.provider;

import com.mylog.statistics.service.StatisticsCache;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class RedisStatisticsCache implements StatisticsCache {

    private static final String PREFIX = "mylog:cache:statistics:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisStatisticsCache(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public String version(UUID userId) {
        try {
            String value = redis.opsForValue().get(versionKey(userId));
            return value == null ? "0" : value;
        } catch (DataAccessException exception) {
            return "0";
        }
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            String value = redis.opsForValue().get(PREFIX + key);
            return value == null ? Optional.empty() : Optional.of(objectMapper.readValue(value, type));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        try {
            redis.opsForValue().set(PREFIX + key, objectMapper.writeValueAsString(value), ttl);
        } catch (RuntimeException ignored) {
            // Cache is best effort; PostgreSQL remains the source of truth.
        }
    }

    @Override
    public void invalidate(UUID userId) {
        try {
            redis.opsForValue().increment(versionKey(userId));
        } catch (DataAccessException ignored) {
            // A failed invalidation only loses cache efficiency until TTL expiry.
        }
    }

    private String versionKey(UUID userId) {
        return PREFIX + "version:" + userId;
    }
}
