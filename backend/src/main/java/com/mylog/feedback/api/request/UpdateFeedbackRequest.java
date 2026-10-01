package com.mylog.feedback.api.request;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
public record UpdateFeedbackRequest(@NotBlank String status,UUID assignedTo) {}
