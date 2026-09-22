package com.mylog.identity.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;

public record LoginRequest(
        @NotBlank @Size(max = 320) String email,
        @NotBlank @Size(max = 72) String password) {

    @AssertTrue(message = "Email must be valid")
    public boolean isEmailValid() {
        return email == null || email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    @AssertTrue(message = "Password must not exceed 72 UTF-8 bytes")
    public boolean isPasswordWithinBcryptLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    @Override
    public String toString() {
        return "LoginRequest[REDACTED]";
    }
}
