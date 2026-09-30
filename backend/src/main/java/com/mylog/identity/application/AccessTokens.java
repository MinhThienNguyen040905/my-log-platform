package com.mylog.identity.application;

import java.util.UUID;

public interface AccessTokens {
    String issue(UUID userId, UUID sessionId);
}
