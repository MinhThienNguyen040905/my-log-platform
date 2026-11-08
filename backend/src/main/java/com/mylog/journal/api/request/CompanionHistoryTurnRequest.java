package com.mylog.journal.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CompanionHistoryTurnRequest(@NotBlank @Pattern(regexp = "assistant|user") String role,
                                          @NotBlank @Size(max = 300) String text) {}
