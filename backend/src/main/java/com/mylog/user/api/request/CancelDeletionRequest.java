package com.mylog.user.api.request;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
public record CancelDeletionRequest(@Email @NotBlank String email,@NotBlank String password) {}
