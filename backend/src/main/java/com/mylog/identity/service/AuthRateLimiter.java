package com.mylog.identity.service;

import com.mylog.identity.config.AuthRateLimitProperties;
import com.mylog.common.api.ApiErrorCodes;
import com.mylog.common.exception.RateLimitExceededException;
import com.mylog.common.exception.ServiceUnavailableException;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class AuthRateLimiter {

    private final RateLimitStore rateLimitStore;
    private final AuthRateLimitProperties properties;
    private final ClientFingerprintService fingerprintService;

    public AuthRateLimiter(
            RateLimitStore rateLimitStore,
            AuthRateLimitProperties properties,
            ClientFingerprintService fingerprintService) {
        this.rateLimitStore = rateLimitStore;
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
            long attempts = rateLimitStore.increment(key, window);
            if (attempts > maximumAttempts) {
                throw new RateLimitExceededException(window);
            }
        } catch (RateLimitExceededException exception) {
            throw exception;
        } catch (RateLimitStoreException exception) {
            throw new ServiceUnavailableException(
                    ApiErrorCodes.DEPENDENCY_UNAVAILABLE,
                    "Authentication service is temporarily unavailable",
                    exception);
        }
    }
}
