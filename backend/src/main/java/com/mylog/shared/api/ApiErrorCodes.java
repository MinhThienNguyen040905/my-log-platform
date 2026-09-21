package com.mylog.shared.api;

public final class ApiErrorCodes {

    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String ANALYSIS_NOT_READY = "ANALYSIS_NOT_READY";
    public static final String ANALYSIS_NOT_RETRYABLE = "ANALYSIS_NOT_RETRYABLE";
    public static final String ACCOUNT_UNAVAILABLE = "ACCOUNT_UNAVAILABLE";
    public static final String AUTHENTICATION_REQUIRED = "AUTHENTICATION_REQUIRED";
    public static final String CONFLICT = "CONFLICT";
    public static final String DEPENDENCY_UNAVAILABLE = "DEPENDENCY_UNAVAILABLE";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String INVALID_REQUEST = "INVALID_REQUEST";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String INVALID_CURSOR = "INVALID_CURSOR";
    public static final String INVALID_IDEMPOTENCY_KEY = "INVALID_IDEMPOTENCY_KEY";
    public static final String INVALID_REFRESH_TOKEN = "INVALID_REFRESH_TOKEN";
    public static final String IDEMPOTENCY_KEY_REUSED = "IDEMPOTENCY_KEY_REUSED";
    public static final String IDEMPOTENCY_REQUEST_IN_PROGRESS = "IDEMPOTENCY_REQUEST_IN_PROGRESS";
    public static final String JOURNAL_NOT_FOUND = "JOURNAL_NOT_FOUND";
    public static final String JOURNAL_VERSION_CONFLICT = "JOURNAL_VERSION_CONFLICT";
    public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
    public static final String UNSUPPORTED_MEDIA_TYPE = "UNSUPPORTED_MEDIA_TYPE";
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";

    private ApiErrorCodes() {}
}
