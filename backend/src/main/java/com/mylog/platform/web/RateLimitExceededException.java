package com.mylog.platform.web;

public final class RateLimitExceededException extends ApplicationException {
    public RateLimitExceededException() {
        super(ErrorCode.RATE_LIMITED, "Vui lòng thử lại sau.");
    }
}
