package com.mylog.journal.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@IdClass(JournalEntryTagId.class)
@Table(name = "journal_entry_tags")
class JournalEntryTag {
    @Id @Column(name = "journal_entry_id") UUID journalEntryId;
    @Id @Column(name = "tag_id") UUID tagId;
    @Column(name = "created_at", nullable = false) Instant createdAt;
}
