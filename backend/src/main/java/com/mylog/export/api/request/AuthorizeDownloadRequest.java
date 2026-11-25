package com.mylog.export.api.request;

import jakarta.validation.constraints.Size;

/** {@code password} is required for accounts that have one; passwordless (Google) accounts need a recent sign-in. */
public record AuthorizeDownloadRequest(@Size(max = 200) String password) {}
