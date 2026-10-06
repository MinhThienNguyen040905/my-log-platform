package com.mylog.knowledge.api.request;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record WriteKnowledgeVersionRequest(@NotBlank String title, @NotBlank String content,
                                           Instant effectiveFrom, Instant effectiveTo) {}
