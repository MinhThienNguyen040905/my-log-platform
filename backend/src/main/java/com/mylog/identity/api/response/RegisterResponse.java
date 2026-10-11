package com.mylog.identity.api.response;

import java.util.UUID;

public record RegisterResponse(UUID registrationId, boolean emailVerificationRequired) {}
