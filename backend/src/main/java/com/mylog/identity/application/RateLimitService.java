package com.mylog.identity.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
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

