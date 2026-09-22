package com.mylog.identity.security;

import com.mylog.identity.service.RateLimitStore;
import com.mylog.identity.service.RateLimitStoreException;
import java.time.Duration;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisRateLimitStore implements RateLimitStore {

    private static final DefaultRedisScript<Long> FIXED_WINDOW_SCRIPT = new DefaultRedisScript<>(
            """
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
              redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """,
            Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisRateLimitStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public long increment(String key, Duration window) {
        try {
            Long attempts = redisTemplate.execute(
                    FIXED_WINDOW_SCRIPT,
                    List.of(key),
                    Long.toString(Math.max(1, window.toSeconds())));
            return attempts == null ? 0 : attempts;
        } catch (DataAccessException exception) {
            throw new RateLimitStoreException("Redis rate-limit store is unavailable", exception);
        }
    }
}
