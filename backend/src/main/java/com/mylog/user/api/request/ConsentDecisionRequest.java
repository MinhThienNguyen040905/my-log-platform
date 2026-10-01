package com.mylog.user.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConsentDecisionRequest(@NotBlank @Size(max = 40) String documentVersion, boolean granted) {}
