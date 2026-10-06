package com.mylog.knowledge.application.query;

import java.time.Instant;
import java.util.UUID;

public record KnowledgeVersionView(UUID itemId, UUID versionId, int version, String slug, String topicCode,
                                   String locale, String title, String content, String status,
                                   String checksum, String chunkStrategyVersion, Instant effectiveFrom,
                                   Instant effectiveTo) {}
