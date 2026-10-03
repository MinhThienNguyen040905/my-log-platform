package com.mylog.knowledge.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "knowledge_embeddings")
public class KnowledgeEmbedding {
    @Id public UUID id;
    @Column(name = "chunk_id", nullable = false) public UUID chunkId;
    @Column(nullable = false) public String provider;
    @Column(nullable = false) public String model;
    @Column(name = "model_version", nullable = false) public String modelVersion;
    @Column(nullable = false) public int dimensions;
    // Vector payload is written by a provider adapter with native SQL after model selection.
    @Column(name = "created_at", nullable = false) public Instant createdAt;
}
