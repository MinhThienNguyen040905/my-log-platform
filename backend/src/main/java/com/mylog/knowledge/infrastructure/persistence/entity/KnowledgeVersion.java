package com.mylog.knowledge.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "knowledge_versions")
public class KnowledgeVersion {
    @Id public UUID id;
    @Column(name = "item_id", nullable = false) public UUID itemId;
    @Column(nullable = false) public int version;
    @Column(nullable = false) public String title;
    @Column(nullable = false, columnDefinition = "text") public String content;
    @Column(name = "content_sha256", nullable = false) public byte[] contentSha256;
    @Column(nullable = false) public String status;
    @Column(name = "review_notes") public String reviewNotes;
    @Column(name = "chunk_strategy_version", nullable = false) public String chunkStrategyVersion;
    @Column(name = "created_by") public UUID createdBy;
    @Column(name = "reviewed_by") public UUID reviewedBy;
    @Column(name = "approved_by") public UUID approvedBy;
    @Column(name = "approved_at") public Instant approvedAt;
    @Column(name = "effective_from") public Instant effectiveFrom;
    @Column(name = "effective_to") public Instant effectiveTo;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
    @Column(name = "updated_at", nullable = false) public Instant updatedAt;
}
