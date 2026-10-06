package com.mylog.knowledge.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity @Table(name = "knowledge_chunks")
public class KnowledgeChunk {
    @Id public UUID id;
    @Column(name = "knowledge_version_id", nullable = false) public UUID knowledgeVersionId;
    @Column(name = "chunk_index", nullable = false) public int chunkIndex;
    @Column(nullable = false, columnDefinition = "text") public String content;
    @Column(name = "token_count", nullable = false) public int tokenCount;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") public Map<String,Object> metadata;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
}
