package com.mylog.journal.application;

import com.mylog.platform.id.IdGenerator;
import com.mylog.platform.web.InvalidRequestException;
import com.mylog.platform.web.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class JournalAssetService {
    private static final Logger log = LoggerFactory.getLogger(JournalAssetService.class);
    private final JournalAssetStore store;
    private final JournalAssetStorage storage;
    private final ImageInspectionService inspection;
    private final IdGenerator ids;
    private final Clock clock;
    private final long maxQuotaBytes;
    private final AssetDeletionQueue deletionQueue;

    // Rate limiting: max 30 uploads per hour per user in memory
    private final Map<UUID, UploadWindow> userRateLimits = new ConcurrentHashMap<>();

    public JournalAssetService(
            JournalAssetStore store,
            JournalAssetStorage storage,
            ImageInspectionService inspection,
            IdGenerator ids,
            Clock clock,
            @Value("${mylog.assets.max-user-quota-bytes:209715200}") long maxQuotaBytes,
            AssetDeletionQueue deletionQueue
    ) {
        this.store = store;
        this.storage = storage;
        this.inspection = inspection;
        this.ids = ids;
        this.clock = clock;
        this.maxQuotaBytes = maxQuotaBytes;
        this.deletionQueue = deletionQueue;
    }

    @Transactional
    public JournalAssetUploadedView uploadDraftAsset(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Tệp tải lên không được để trống.");
        }

        checkRateLimit(userId);

        long currentUsage = store.sumActiveSizeBytesByUserId(userId);
        if (currentUsage + file.getSize() > maxQuotaBytes) {
            throw new InvalidRequestException("Dung lượng tải ảnh vượt quá hạn mức tối đa cho phép (200MB).");
        }

        byte[] rawBytes;
        try {
            rawBytes = file.getBytes();
        } catch (IOException e) {
            throw new InvalidRequestException("Không thể đọc dữ liệu tệp tải lên.");
        }

        SanitizedImage sanitized = inspection.inspectAndSanitize(rawBytes, file.getContentType());
        UUID assetId = ids.next();
        AssetStoredResult stored = storage.store(userId, assetId, sanitized.bytes(), sanitized.format(), sanitized.mimeType());

        Instant now = clock.instant();
        JournalAssetData asset = new JournalAssetData();
        asset.id = assetId;
        asset.userId = userId;
        asset.journalEntryId = null;
        asset.providerAssetId = stored.providerAssetId();
        asset.publicId = stored.publicId();
        asset.providerVersion = stored.providerVersion();
        asset.format = sanitized.format();
        asset.deliveryType = stored.deliveryType();
        asset.assetType = "IMAGE";
        asset.mimeType = sanitized.mimeType();
        asset.sizeBytes = sanitized.bytes().length;
        asset.sha256 = sanitized.sha256();
        asset.width = sanitized.width();
        asset.height = sanitized.height();
        asset.position = 0;
        asset.isCover = false;
        asset.status = JournalAssetData.STATUS_PENDING_ATTACH;
        asset.createdAt = now;

        store.save(asset);

        String temporarySignedUrl = storage.generateSignedUrl(asset.publicId, asset.format, asset.deliveryType, Duration.ofHours(1));
        return new JournalAssetUploadedView(
                asset.id,
                asset.format,
                asset.mimeType,
                asset.width,
                asset.height,
                asset.sizeBytes,
                temporarySignedUrl
        );
    }

    @Transactional(readOnly = true)
    public String getSignedUrl(UUID userId, UUID assetId) {
        JournalAssetData asset = store.findByUserIdAndId(userId, assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hình ảnh."));
        if (JournalAssetData.STATUS_DELETED.equals(asset.status)) {
            throw new ResourceNotFoundException("Hình ảnh đã bị xóa.");
        }
        return storage.generateSignedUrl(asset.publicId, asset.format, asset.deliveryType, Duration.ofHours(1));
    }

    @Transactional(readOnly = true)
    public Map<UUID, String> batchGetSignedUrls(UUID userId, List<UUID> assetIds) {
        if (assetIds == null || assetIds.isEmpty()) return Map.of();
        List<JournalAssetData> assets = store.findAssetsByUserIdAndIds(userId, assetIds);
        Map<UUID, String> result = new HashMap<>();
        for (JournalAssetData a : assets) {
            if (!JournalAssetData.STATUS_DELETED.equals(a.status)) {
                String url = storage.generateSignedUrl(a.publicId, a.format, a.deliveryType, Duration.ofHours(1));
                result.put(a.id, url);
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public JsonNode enrichContentWithSignedUrls(UUID userId, JsonNode contentJson) {
        if (contentJson == null) return null;
        List<UUID> assetIds = TipTapContent.extractAssetIds(contentJson);
        if (assetIds.isEmpty()) return contentJson;
        Map<UUID, String> urls = batchGetSignedUrls(userId, assetIds);
        return TipTapContent.enrichImageUrls(contentJson, urls);
    }

    public String resolveCoverUrl(JournalAssetData asset) {
        if (asset == null || JournalAssetData.STATUS_DELETED.equals(asset.status)) return null;
        return storage.generateThumbnailUrl(asset.publicId, asset.format, asset.deliveryType, 400, 400, Duration.ofHours(1));
    }

    private void checkRateLimit(UUID userId) {
        Instant now = clock.instant();
        UploadWindow window = userRateLimits.compute(userId, (k, v) -> {
            if (v == null || now.isAfter(v.windowStart.plus(Duration.ofHours(1)))) {
                return new UploadWindow(now, 1);
            }
            return new UploadWindow(v.windowStart, v.count + 1);
        });
        if (window.count > 30) {
            throw new InvalidRequestException("Đã vượt quá giới hạn tải lên tối đa 30 ảnh mỗi giờ. Vui lòng thử lại sau.");
        }
    }

    /** A file kept in asset storage, enough to delete it after its database rows are gone. */
    public record StoredFile(String publicId, String assetType, String deliveryType) {}

    /**
     * Queues every file the user still has in storage for deletion. Call in the same transaction that deletes the
     * user's asset rows ("start over", account deletion): the queue then outlives a storage outage, and
     * {@code AssetDeletionQueueJob} removes the files with retries.
     *
     * @return the number of files queued
     */
    @Transactional
    public int enqueueAllForDeletion(UUID userId) {
        List<StoredFile> files = store.findStoredFilesByUser(userId);
        deletionQueue.enqueue(files, clock.instant());
        return files.size();
    }

    private record UploadWindow(Instant windowStart, int count) {}
}
