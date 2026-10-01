package com.mylog.knowledge.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "knowledge_embeddings")
class KnowledgeEmbedding {
    @Id UUID id;
    @Column(name = "chunk_id", nullable = false) UUID chunkId;
    @Column(nullable = false) String provider;
    @Column(nullable = false) String model;
    @Column(name = "model_version", nullable = false) String modelVersion;
    @Column(nullable = false) int dimensions;
    // Vector payload is written by a provider adapter with native SQL after model selection.
    @Column(name = "created_at", nullable = false) Instant createdAt;
}
