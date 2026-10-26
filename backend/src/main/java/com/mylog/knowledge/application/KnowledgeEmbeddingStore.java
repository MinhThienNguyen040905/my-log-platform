package com.mylog.knowledge.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface KnowledgeEmbeddingStore {
    record ModelSignature(String provider, String model, String modelVersion, int dimensions) {}
    record PendingChunk(UUID chunkId, String title, String text) {}
    record Embedding(UUID chunkId, float[] vector) {}
    record Match(UUID itemId, UUID versionId, int version, UUID chunkId, String text,
                 String sourceName, String sourceUrl, double similarity) {}

    void save(ModelSignature signature, List<Embedding> embeddings, Instant now);

    List<PendingChunk> pending(ModelSignature signature, int limit);

    List<Match> search(float[] queryVector, ModelSignature signature, String topicCode,
                       String locale, Instant now, int limit);
}
