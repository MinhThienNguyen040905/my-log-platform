package com.mylog.analysis.application;

import java.util.List;
import java.util.UUID;

public interface KnowledgeRetriever {
    record Passage(UUID itemId, UUID versionId, int version, UUID chunkId, String text,
                   String sourceName, String sourceUrl) {}
    List<Passage> retrieve(String topicCode, String locale, int limit);
}
