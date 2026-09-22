package com.mylog.common.security;

import java.util.UUID;

public interface AccountStatusVerifier {

    boolean isActive(UUID userId);
}
