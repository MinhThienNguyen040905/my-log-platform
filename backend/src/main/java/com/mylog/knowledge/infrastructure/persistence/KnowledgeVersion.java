package com.mylog.knowledge.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "knowledge_versions")
class KnowledgeVersion {
    @Id UUID id;
    @Column(name = "item_id", nullable = false) UUID itemId;
    @Column(nullable = false) int version;
    @Column(nullable = false) String title;
    @Column(nullable = false, columnDefinition = "text") String content;
    @Column(name = "content_sha256", nullable = false) byte[] contentSha256;
    @Column(nullable = false) String status;
    @Column(name = "review_notes") String reviewNotes;
    @Column(name = "chunk_strategy_version", nullable = false) String chunkStrategyVersion;
    @Column(name = "created_by") UUID createdBy;
    @Column(name = "reviewed_by") UUID reviewedBy;
    @Column(name = "approved_by") UUID approvedBy;
    @Column(name = "approved_at") Instant approvedAt;
    @Column(name = "effective_from") Instant effectiveFrom;
    @Column(name = "effective_to") Instant effectiveTo;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;
}
