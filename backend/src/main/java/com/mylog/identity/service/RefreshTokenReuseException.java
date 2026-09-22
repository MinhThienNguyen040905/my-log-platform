package com.mylog.identity.service;

import com.mylog.common.api.ApiErrorCodes;
import com.mylog.common.exception.UnauthorizedException;

public class RefreshTokenReuseException extends UnauthorizedException {

    public RefreshTokenReuseException() {
        super(ApiErrorCodes.INVALID_REFRESH_TOKEN, "Refresh token is invalid");
    }
}
