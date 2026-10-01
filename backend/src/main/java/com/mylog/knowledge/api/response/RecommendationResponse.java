package com.mylog.knowledge.api.response;

import com.mylog.analysis.application.KnowledgeRetriever;
import java.util.List;
import java.util.UUID;

public record RecommendationResponse(String topicCode, List<Excerpt> excerpts) {
    public record Excerpt(String text, Citation citation) {}
    public record Citation(UUID itemId, UUID versionId, int version, UUID chunkId,
                           String sourceName, String sourceUrl) {}
    public static RecommendationResponse from(String topicCode, List<KnowledgeRetriever.Passage> passages) {
        return new RecommendationResponse(topicCode,passages.stream().map(p -> new Excerpt(p.text(),
                new Citation(p.itemId(),p.versionId(),p.version(),p.chunkId(),p.sourceName(),p.sourceUrl()))).toList());
    }
}
