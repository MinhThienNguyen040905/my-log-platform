package com.mylog.journal.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WritingSuggestionRequest(@NotBlank @Size(max = 4000) String text) {}
