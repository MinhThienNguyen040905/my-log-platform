package com.mylog.identity.service;

import java.time.Duration;

public interface RateLimitStore {

    long increment(String key, Duration window);
}
