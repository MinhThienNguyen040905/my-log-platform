package com.mylog.knowledge.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity @Table(name = "knowledge_chunks")
class KnowledgeChunk {
    @Id UUID id;
    @Column(name = "knowledge_version_id", nullable = false) UUID knowledgeVersionId;
    @Column(name = "chunk_index", nullable = false) int chunkIndex;
    @Column(nullable = false, columnDefinition = "text") String content;
    @Column(name = "token_count", nullable = false) int tokenCount;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") Map<String,Object> metadata;
    @Column(name = "created_at", nullable = false) Instant createdAt;
}
