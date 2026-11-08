package com.mylog.journal.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "journal_assets")
public class JournalAsset {
    public static final String STATUS_PENDING_ATTACH = "PENDING_ATTACH";
    public static final String STATUS_PENDING_SCAN = "PENDING_SCAN";
    public static final String STATUS_READY = "READY";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_DELETED = "DELETED";

    @Id public UUID id;
    @Column(name = "journal_entry_id") public UUID journalEntryId;
    @Column(name = "user_id", nullable = false) public UUID userId;
    @Column(name = "provider_asset_id", nullable = false) public String providerAssetId;
    @Column(name = "public_id", nullable = false) public String publicId;
    @Column(name = "provider_version", nullable = false) public long providerVersion;
    @Column(nullable = false) public String format;
    @Column(name = "delivery_type", nullable = false) public String deliveryType;
    @Column(name = "asset_type", nullable = false) public String assetType;
    @Column(name = "mime_type", nullable = false) public String mimeType;
    @Column(name = "size_bytes", nullable = false) public long sizeBytes;
    @Column(nullable = false) public byte[] sha256;
    public Integer width;
    public Integer height;
    @Column(nullable = false) public int position;
    @Column(name = "is_cover", nullable = false) public boolean isCover;
    @Column(nullable = false) public String status;
    @Column(name = "encrypted_caption") public byte[] encryptedCaption;
    @Column(name = "caption_iv") public byte[] captionIv;
    @Column(name = "caption_wrapped_key") public byte[] captionWrappedKey;
    @Column(name = "caption_key_version") public String captionKeyVersion;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
    @Column(name = "deleted_at") public Instant deletedAt;
}
