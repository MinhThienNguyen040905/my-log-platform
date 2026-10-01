package com.mylog.support;

import com.mylog.platform.security.CurrentUser;
import com.mylog.platform.security.CurrentUserProvider;

import java.util.Optional;

public final class FixedCurrentUserProvider implements CurrentUserProvider {
    private final CurrentUser currentUser;

    public FixedCurrentUserProvider(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    @Override
    public Optional<CurrentUser> findCurrent() {
        return Optional.ofNullable(currentUser);
    }
}
