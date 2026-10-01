package com.mylog.knowledge.api.request;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record CreateKnowledgeItemRequest(@NotBlank String slug, @NotBlank String topicCode,
        @NotBlank String locale, @NotBlank String sourceName, String sourceUrl,
        @NotBlank String ownerTeam, @NotBlank String title, @NotBlank String content,
        Instant effectiveFrom, Instant effectiveTo) {}
