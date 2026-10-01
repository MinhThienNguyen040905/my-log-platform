package com.mylog.user.api.request;
import jakarta.validation.constraints.NotBlank;
public record RequestDeletionRequest(@NotBlank String password) {}
