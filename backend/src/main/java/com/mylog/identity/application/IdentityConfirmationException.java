package com.mylog.identity.application;

import com.mylog.platform.web.ApplicationException;
import com.mylog.platform.web.ErrorCode;

/**
 * A sensitive action was refused because the user did not confirm their identity: {@code PASSWORD_INCORRECT} or
 * {@code RECENT_LOGIN_REQUIRED}. Mapped to 403 rather than 401 so a wrong password never ends the user's session.
 */
public class IdentityConfirmationException extends ApplicationException {
    public static final String PASSWORD_INCORRECT = "PASSWORD_INCORRECT";
    public static final String RECENT_LOGIN_REQUIRED = "RECENT_LOGIN_REQUIRED";

    public IdentityConfirmationException(String reason) { super(ErrorCode.FORBIDDEN, reason); }
}
