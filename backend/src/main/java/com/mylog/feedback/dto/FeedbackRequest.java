package com.mylog.feedback.dto;

import jakarta.validation.constraints.NotBlank;

public record FeedbackRequest(@NotBlank String value) {}
