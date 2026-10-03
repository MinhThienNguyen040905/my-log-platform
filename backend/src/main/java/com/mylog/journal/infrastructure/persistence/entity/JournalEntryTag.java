package com.mylog.journal.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@IdClass(JournalEntryTagId.class)
@Table(name = "journal_entry_tags")
public class JournalEntryTag {
    @Id @Column(name = "journal_entry_id") public UUID journalEntryId;
    @Id @Column(name = "tag_id") public UUID tagId;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
}
