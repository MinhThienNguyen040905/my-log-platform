package com.mylog.feedback.api.request;
import jakarta.validation.constraints.NotBlank;
public record CreateFeedbackRequest(@NotBlank String category,@NotBlank String message) {}
