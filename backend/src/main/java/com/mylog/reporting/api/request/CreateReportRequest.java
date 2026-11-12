package com.mylog.reporting.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Asks for the report of the most recent finished week or month. */
public record CreateReportRequest(@NotBlank @Pattern(regexp = "WEEKLY|MONTHLY") String type) {}
