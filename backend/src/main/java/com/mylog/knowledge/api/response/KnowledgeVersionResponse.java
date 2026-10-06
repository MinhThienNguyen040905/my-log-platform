package com.mylog.knowledge.api.response;

import com.mylog.knowledge.application.query.KnowledgeVersionView;
import java.time.Instant;
import java.util.UUID;

public record KnowledgeVersionResponse(UUID itemId, UUID versionId, int version, String slug,
        String topicCode, String locale, String title, String content, String status, String checksum,
        String chunkStrategyVersion, Instant effectiveFrom, Instant effectiveTo) {
    public static KnowledgeVersionResponse from(KnowledgeVersionView v) {
        return new KnowledgeVersionResponse(v.itemId(),v.versionId(),v.version(),v.slug(),v.topicCode(),
                v.locale(),v.title(),v.content(),v.status(),v.checksum(),v.chunkStrategyVersion(),
                v.effectiveFrom(),v.effectiveTo());
    }
}
