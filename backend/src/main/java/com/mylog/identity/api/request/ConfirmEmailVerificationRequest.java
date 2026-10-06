package com.mylog.identity.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmEmailVerificationRequest(@NotBlank @Size(max = 512) String token) {}
