package com.mylog.identity.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleAuthRequest(
        @NotBlank(message = "Google authorization code không được để trống")
        String code,

        @Size(max = 64)
        String timezone,

        @Size(max = 16)
        String locale,

        @Size(max = 120)
        String deviceName
) {
}

