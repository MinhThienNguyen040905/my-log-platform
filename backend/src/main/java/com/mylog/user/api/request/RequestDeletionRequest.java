package com.mylog.user.api.request;

import jakarta.validation.constraints.Size;

/** {@code password} is required for accounts that have one; passwordless (Google) accounts need a recent sign-in. */
public record RequestDeletionRequest(@Size(max = 200) String password) {}
