package com.mylog.journal.application;

import java.time.Instant;
import java.util.UUID;

/**
 * A journal image as the application layer sees it. The persistence adapter maps it to and from the JPA entity,
 * so services and controllers never touch the entity; changes reach the database only through
 * {@link JournalAssetStore#save(JournalAssetData)} or the store's update methods.
 */
public class JournalAssetData {
    public static final String STATUS_PENDING_ATTACH = "PENDING_ATTACH";
    public static final String STATUS_PENDING_SCAN = "PENDING_SCAN";
    public static final String STATUS_READY = "READY";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_DELETED = "DELETED";

    public UUID id;
    public UUID journalEntryId;
    public UUID userId;
    public String providerAssetId;
    public String publicId;
    public long providerVersion;
    public String format;
    public String deliveryType;
    public String assetType;
    public String mimeType;
    public long sizeBytes;
    public byte[] sha256;
    public Integer width;
    public Integer height;
    public int position;
    public boolean isCover;
    public String status;
    public byte[] encryptedCaption;
    public byte[] captionIv;
    public byte[] captionWrappedKey;
    public String captionKeyVersion;
    public Instant createdAt;
    public Instant deletedAt;
}
