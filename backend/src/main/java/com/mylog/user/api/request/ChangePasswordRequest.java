package com.mylog.user.api.request;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "current-password-required")
        String currentPassword,

        @NotBlank(message = "new-password-required")
        String newPassword
) {
    @Override
    public String toString() {
        return "ChangePasswordRequest[currentPassword=***, newPassword=***]";
    }
}

