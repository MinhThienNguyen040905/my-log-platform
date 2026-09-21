package com.mylog.identity.application;

import com.mylog.identity.configuration.AuthRateLimitProperties;
import com.mylog.shared.api.ApiErrorCodes;
import com.mylog.shared.exception.RateLimitExceededException;
import com.mylog.shared.exception.ServiceUnavailableException;
import java.time.Duration;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class AuthRateLimiter {

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
    private final AuthRateLimitProperties properties;
    private final ClientFingerprintService fingerprintService;

    public AuthRateLimiter(
            StringRedisTemplate redisTemplate,
            AuthRateLimitProperties properties,
            ClientFingerprintService fingerprintService) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.fingerprintService = fingerprintService;
    }

    public void checkRegister(String ipAddress) {
        check("register", ipAddress, properties.registerAttempts());
    }

    public void checkLogin(String ipAddress) {
        check("login", ipAddress, properties.loginAttempts());
    }

    public void checkRefresh(String ipAddress) {
        check("refresh", ipAddress, properties.refreshAttempts());
    }

    private void check(String operation, String ipAddress, int maximumAttempts) {
        Duration window = properties.window();
        String fingerprint = fingerprintService.hash(ipAddress == null ? "unknown" : ipAddress);
        String key = properties.keyPrefix() + ":" + operation + ":" + fingerprint;
        try {
            Long attempts = redisTemplate.execute(
                    FIXED_WINDOW_SCRIPT,
                    List.of(key),
                    Long.toString(Math.max(1, window.toSeconds())));
            if (attempts != null && attempts > maximumAttempts) {
                throw new RateLimitExceededException(window);
            }
        } catch (RateLimitExceededException exception) {
            throw exception;
        } catch (DataAccessException exception) {
            throw new ServiceUnavailableException(
                    ApiErrorCodes.DEPENDENCY_UNAVAILABLE,
                    "Authentication service is temporarily unavailable",
                    exception);
        }
    }
}
