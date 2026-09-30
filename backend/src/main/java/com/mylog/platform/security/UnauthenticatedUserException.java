package com.mylog.platform.security;

public final class UnauthenticatedUserException extends RuntimeException {
    public UnauthenticatedUserException() {
        super("Authentication is required");
    }
}
