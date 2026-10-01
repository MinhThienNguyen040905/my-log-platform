package com.mylog.journal.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "journal_tags")
class JournalTag {
    @Id UUID id;
    @Column(name = "user_id", nullable = false) UUID userId;
    @Column(name = "name_lookup_hash", nullable = false) byte[] nameLookupHash;
    @Column(name = "encrypted_name", nullable = false) byte[] encryptedName;
    @Column(name = "name_iv", nullable = false) byte[] nameIv;
    @Column(name = "name_wrapped_key", nullable = false) byte[] nameWrappedKey;
    @Column(name = "name_key_version", nullable = false) String nameKeyVersion;
    String color;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;
}
