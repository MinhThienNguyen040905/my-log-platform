package com.mylog.platform.web;

public abstract class ApplicationException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String safeDetail;

    protected ApplicationException(ErrorCode errorCode, String safeDetail) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.safeDetail = safeDetail;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public String safeDetail() {
        return safeDetail;
    }
}
