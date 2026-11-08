package com.mylog.identity.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmPasswordResetRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 12, max = 72) String newPassword
) {}

