package com.mylog.identity.service;

public class RateLimitStoreException extends RuntimeException {

    public RateLimitStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
