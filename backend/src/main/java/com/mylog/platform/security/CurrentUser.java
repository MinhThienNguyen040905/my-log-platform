package com.mylog.platform.security;

import java.util.Set;
import java.util.UUID;

public record CurrentUser(UUID userId, Set<String> authorities) {
    public CurrentUser {
        authorities = Set.copyOf(authorities);
    }
}
