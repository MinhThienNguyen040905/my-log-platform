package com.mylog.knowledge.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "journal_prompts")
public class JournalPrompt {
    @Id public UUID id;
    @Column(nullable = false) public String code;
    @Column(nullable = false) public String locale;
    @Column(nullable = false) public String category;
    @Column(name = "prompt_text", nullable = false, columnDefinition = "text") public String promptText;
    @Column(nullable = false) public String status;
    @Column(name = "display_order", nullable = false) public int displayOrder;
    @Column(name = "valid_from") public Instant validFrom;
    @Column(name = "valid_to") public Instant validTo;
    @Column(name = "created_by") public UUID createdBy;
    @Column(name = "updated_by") public UUID updatedBy;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
    @Column(name = "updated_at", nullable = false) public Instant updatedAt;
}
