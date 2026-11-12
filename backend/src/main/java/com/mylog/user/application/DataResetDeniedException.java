package com.mylog.user.application;

import com.mylog.platform.web.ApplicationException;
import com.mylog.platform.web.ErrorCode;

/**
 * The data reset was refused: {@code PASSWORD_INCORRECT} or {@code RECENT_LOGIN_REQUIRED}. Mapped to 403 rather
 * than 401 so a wrong password never ends the user's session.
 */
public class DataResetDeniedException extends ApplicationException {
    public DataResetDeniedException(String reason) { super(ErrorCode.FORBIDDEN, reason); }
}
