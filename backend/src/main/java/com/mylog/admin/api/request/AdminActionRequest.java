package com.mylog.admin.api.request;

import jakarta.validation.constraints.NotBlank;

public record AdminActionRequest(@NotBlank String reasonCode) {}
