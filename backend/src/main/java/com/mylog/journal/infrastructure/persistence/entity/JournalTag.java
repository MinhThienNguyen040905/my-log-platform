package com.mylog.journal.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "journal_tags")
public class JournalTag {
    @Id public UUID id;
    @Column(name = "user_id", nullable = false) public UUID userId;
    @Column(name = "name_lookup_hash", nullable = false) public byte[] nameLookupHash;
    @Column(name = "encrypted_name", nullable = false) public byte[] encryptedName;
    @Column(name = "name_iv", nullable = false) public byte[] nameIv;
    @Column(name = "name_wrapped_key", nullable = false) public byte[] nameWrappedKey;
    @Column(name = "name_key_version", nullable = false) public String nameKeyVersion;
    public String color;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
    @Column(name = "updated_at", nullable = false) public Instant updatedAt;
}
