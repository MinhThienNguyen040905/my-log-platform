package com.mylog.identity.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Same condition as the {@code IdentityStore} it depends on. */
@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class RateLimitService {
    private final IdentityStore store;

    public RateLimitService(IdentityStore store) {
        this.store = store;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedAttempt(byte[] subjectHash, Instant now, int maxAttempts, long windowSeconds) {
        store.recordRateLimit(subjectHash, now, maxAttempts, windowSeconds);
    }
}

