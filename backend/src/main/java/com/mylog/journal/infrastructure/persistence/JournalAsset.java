package com.mylog.journal.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "journal_assets")
class JournalAsset {
    @Id UUID id;
    @Column(name = "journal_entry_id", nullable = false) UUID journalEntryId;
    @Column(name = "user_id", nullable = false) UUID userId;
    @Column(name = "provider_asset_id", nullable = false) String providerAssetId;
    @Column(name = "public_id", nullable = false) String publicId;
    @Column(name = "provider_version", nullable = false) long providerVersion;
    @Column(nullable = false) String format;
    @Column(name = "delivery_type", nullable = false) String deliveryType;
    @Column(name = "asset_type", nullable = false) String assetType;
    @Column(name = "mime_type", nullable = false) String mimeType;
    @Column(name = "size_bytes", nullable = false) long sizeBytes;
    @Column(nullable = false) byte[] sha256;
    Integer width;
    Integer height;
    @Column(nullable = false) String status;
    @Column(name = "encrypted_caption") byte[] encryptedCaption;
    @Column(name = "caption_iv") byte[] captionIv;
    @Column(name = "caption_wrapped_key") byte[] captionWrappedKey;
    @Column(name = "caption_key_version") String captionKeyVersion;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "deleted_at") Instant deletedAt;
}
