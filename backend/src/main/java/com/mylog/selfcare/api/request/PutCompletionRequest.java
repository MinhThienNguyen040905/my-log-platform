package com.mylog.selfcare.api.request;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PutCompletionRequest(@NotNull BigDecimal value) {}
