package com.mylog.knowledge.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "knowledge_items")
class KnowledgeItem {
    @Id UUID id;
    @Column(nullable = false) String slug;
    @Column(name = "topic_code", nullable = false) String topicCode;
    @Column(nullable = false) String locale;
    @Column(name = "source_name", nullable = false) String sourceName;
    @Column(name = "source_url") String sourceUrl;
    @Column(name = "owner_team", nullable = false) String ownerTeam;
    @Column(nullable = false) String status;
    @Column(name = "created_by") UUID createdBy;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;
}
