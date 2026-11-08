package com.mylog.journal.application;

import java.time.Duration;
import java.util.UUID;

public interface JournalAssetStorage {
    AssetStoredResult store(UUID userId, UUID assetId, byte[] cleanBytes, String format, String mimeType);
    String generateSignedUrl(String publicId, String format, String deliveryType, Duration ttl);
    String generateThumbnailUrl(String publicId, String format, String deliveryType, int width, int height, Duration ttl);
    void delete(String publicId, String assetType, String deliveryType);
}

