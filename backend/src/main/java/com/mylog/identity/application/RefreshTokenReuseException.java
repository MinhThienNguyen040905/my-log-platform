package com.mylog.identity.application;

import com.mylog.shared.api.ApiErrorCodes;
import com.mylog.shared.exception.UnauthorizedException;

public class RefreshTokenReuseException extends UnauthorizedException {

    public RefreshTokenReuseException() {
        super(ApiErrorCodes.INVALID_REFRESH_TOKEN, "Refresh token is invalid");
    }
}
