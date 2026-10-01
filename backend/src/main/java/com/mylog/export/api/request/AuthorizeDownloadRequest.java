package com.mylog.export.api.request;
import jakarta.validation.constraints.NotBlank;
public record AuthorizeDownloadRequest(@NotBlank String password) {}
