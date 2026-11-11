package com.mylog.insight.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Only hiding is supported: the user dismisses an insight they do not find useful. */
public record UpdateInsightRequest(@NotBlank @Pattern(regexp = "HIDDEN") String status) {}
