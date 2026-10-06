package com.mylog.export.api.request;
import jakarta.validation.constraints.NotBlank;
public record CreateExportRequest(@NotBlank String format) {}
