package com.mylog.identity.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;

public record RegisterRequest(
        @NotBlank @Size(max = 320) String email,
        @NotBlank @Size(min = 10, max = 72) String password,
        @NotBlank @Size(max = 100) String displayName,
        @AssertTrue(message = "Terms must be accepted") boolean acceptTerms,
        @AssertTrue(message = "Privacy policy must be accepted") boolean acceptPrivacy) {

    @AssertTrue(message = "Email must be valid")
    public boolean isEmailValid() {
        return email == null || email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    @AssertTrue(message = "Password must contain uppercase, lowercase and numeric characters")
    public boolean isPasswordComplexEnough() {
        if (password == null) {
            return true;
        }
        return password.chars().anyMatch(Character::isUpperCase)
                && password.chars().anyMatch(Character::isLowerCase)
                && password.chars().anyMatch(Character::isDigit);
    }

    @AssertTrue(message = "Password must not exceed 72 UTF-8 bytes")
    public boolean isPasswordWithinBcryptLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    @Override
    public String toString() {
        return "RegisterRequest[REDACTED]";
    }
}
