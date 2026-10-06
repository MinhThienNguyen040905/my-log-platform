package com.mylog.identity.api.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 12, max = 128) String password,
        @NotBlank @Size(max = 64) String timezone,
        @NotBlank @Size(max = 10) String locale,
        @NotBlank @Size(max = 40) String termsVersion,
        @NotBlank @Size(max = 40) String privacyVersion,
        @AssertTrue boolean acceptTerms,
        @AssertTrue boolean acceptPrivacy
) {}
