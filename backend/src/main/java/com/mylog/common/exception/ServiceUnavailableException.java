package com.mylog.common.exception;

public class ServiceUnavailableException extends RuntimeException {

    private final String code;

    public ServiceUnavailableException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
