package com.mylog.identity.application;

import com.mylog.platform.web.ApplicationException;
import com.mylog.platform.web.ErrorCode;

public final class PasswordChangeException extends ApplicationException {
    public PasswordChangeException(String code) {
        super(ErrorCode.VALIDATION_ERROR, code);
    }
}

