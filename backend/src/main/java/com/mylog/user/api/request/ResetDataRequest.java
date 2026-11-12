package com.mylog.user.api.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

/** {@code password} is required for accounts that have one; {@code confirm} must be true. */
public record ResetDataRequest(@Size(max = 200) String password, @AssertTrue boolean confirm) {}
