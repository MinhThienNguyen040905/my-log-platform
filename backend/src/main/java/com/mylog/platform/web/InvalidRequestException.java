package com.mylog.platform.web;

public final class InvalidRequestException extends ApplicationException {
    public InvalidRequestException() { super(ErrorCode.VALIDATION_ERROR, "Dữ liệu không hợp lệ."); }
    public InvalidRequestException(String message) { super(ErrorCode.VALIDATION_ERROR, message); }
}
