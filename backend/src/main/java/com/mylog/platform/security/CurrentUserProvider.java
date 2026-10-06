package com.mylog.platform.security;

import java.util.Optional;

public interface CurrentUserProvider {
    Optional<CurrentUser> findCurrent();

    default CurrentUser requireCurrent() {
        return findCurrent().orElseThrow(UnauthenticatedUserException::new);
    }
}
