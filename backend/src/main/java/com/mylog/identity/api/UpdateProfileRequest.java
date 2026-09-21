package com.mylog.identity.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 1, max = 100) String displayName,
        @Size(min = 1, max = 64) String timezone,
        @Pattern(regexp = "vi|en") String language) {

    @AssertTrue(message = "At least one profile field must be provided")
    public boolean isNotEmpty() {
        return displayName != null || timezone != null || language != null;
    }
}
