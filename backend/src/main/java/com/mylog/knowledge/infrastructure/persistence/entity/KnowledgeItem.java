package com.mylog.knowledge.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "knowledge_items")
public class KnowledgeItem {
    @Id public UUID id;
    @Column(nullable = false) public String slug;
    @Column(name = "topic_code", nullable = false) public String topicCode;
    @Column(nullable = false) public String locale;
    @Column(name = "source_name", nullable = false) public String sourceName;
    @Column(name = "source_url") public String sourceUrl;
    @Column(name = "owner_team", nullable = false) public String ownerTeam;
    @Column(nullable = false) public String status;
    @Column(name = "created_by") public UUID createdBy;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
    @Column(name = "updated_at", nullable = false) public Instant updatedAt;
}
