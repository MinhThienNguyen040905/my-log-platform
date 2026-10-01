package com.mylog.knowledge.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "journal_prompts")
class JournalPrompt {
    @Id UUID id;
    @Column(nullable = false) String code;
    @Column(nullable = false) String locale;
    @Column(nullable = false) String category;
    @Column(name = "prompt_text", nullable = false, columnDefinition = "text") String promptText;
    @Column(nullable = false) String status;
    @Column(name = "display_order", nullable = false) int displayOrder;
    @Column(name = "valid_from") Instant validFrom;
    @Column(name = "valid_to") Instant validTo;
    @Column(name = "created_by") UUID createdBy;
    @Column(name = "updated_by") UUID updatedBy;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;
}
