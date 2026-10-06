package com.mylog.knowledge.api.request;

import jakarta.validation.constraints.NotBlank;

public record KnowledgeReasonRequest(@NotBlank String reasonCode) {}
