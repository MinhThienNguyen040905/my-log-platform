package com.mylog.platform.web;

public final class ConflictException extends ApplicationException {
    public ConflictException(String safeDetail) {
        super(ErrorCode.CONFLICT, safeDetail);
    }
}
