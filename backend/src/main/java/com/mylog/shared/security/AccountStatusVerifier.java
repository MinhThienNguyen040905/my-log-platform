package com.mylog.shared.security;

import java.util.UUID;

public interface AccountStatusVerifier {

    boolean isActive(UUID userId);
}
