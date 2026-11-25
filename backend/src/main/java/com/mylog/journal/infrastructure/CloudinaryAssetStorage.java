package com.mylog.journal.infrastructure;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import com.mylog.journal.application.AssetStoredResult;
import com.mylog.journal.application.JournalAssetStorage;
import com.mylog.platform.storage.CloudinaryProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
@Primary
@ConditionalOnProperty(prefix = "mylog.cloudinary", name = "enabled", havingValue = "true")
public class CloudinaryAssetStorage implements JournalAssetStorage {
    private final ObjectProvider<Cloudinary> cloudinaryProvider;
    private final CloudinaryProperties properties;

    public CloudinaryAssetStorage(ObjectProvider<Cloudinary> cloudinaryProvider, CloudinaryProperties properties) {
        this.cloudinaryProvider = cloudinaryProvider;
        this.properties = properties;
    }

    @Override
    public AssetStoredResult store(UUID userId, UUID assetId, byte[] cleanBytes, String format, String mimeType) {
        Cloudinary cloudinary = requireCloudinary();
        String folder = (properties.folder() == null || properties.folder().isBlank()) ? "mylog" : properties.folder();
        String publicId = folder + "/" + userId + "/" + assetId;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "public_id", publicId,
                    "resource_type", "image",
                    "type", "authenticated",
                    "overwrite", true,
                    "invalidate", true
            );
            Map<?, ?> result = cloudinary.uploader().upload(cleanBytes, uploadParams);
            Object assetIdVal = result.get("asset_id");
            String providerAssetId = assetIdVal != null ? assetIdVal.toString() : assetId.toString();
            long version = result.get("version") instanceof Number n ? n.longValue() : 1L;
            return new AssetStoredResult(providerAssetId, publicId, version, "AUTHENTICATED");
        } catch (IOException e) {
            throw new IllegalStateException("Lỗi khi tải ảnh lên dịch vụ Cloudinary.", e);
        }
    }

    @Override
    public String generateSignedUrl(String publicId, String format, String deliveryType, Duration ttl) {
        Cloudinary cloudinary = requireCloudinary();
        String type = (deliveryType == null ? "authenticated" : deliveryType).toLowerCase(Locale.ROOT);
        return cloudinary.url()
                .resourceType("image")
                .type(type)
                .signed(true)
                .format(format)
                .generate(publicId);
    }

    @Override
    public String generateThumbnailUrl(String publicId, String format, String deliveryType, int width, int height, Duration ttl) {
        Cloudinary cloudinary = requireCloudinary();
        String type = (deliveryType == null ? "authenticated" : deliveryType).toLowerCase(Locale.ROOT);
        return cloudinary.url()
                .resourceType("image")
                .type(type)
                .transformation(new Transformation<>().width(width).height(height).crop("fill").quality("auto").fetchFormat("auto"))
                .signed(true)
                .format(format)
                .generate(publicId);
    }

    /**
     * Deletes the file; "not found" counts as deleted. Throws when Cloudinary is unavailable or refuses, so callers
     * (the deletion queue, the orphan cleanup) can retry instead of losing track of the file.
     */
    @Override
    public void delete(String publicId, String assetType, String deliveryType) {
        Cloudinary cloudinary = requireCloudinary();
        String type = (deliveryType == null ? "authenticated" : deliveryType).toLowerCase(Locale.ROOT);
        Map<?, ?> response;
        try {
            response = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", "IMAGE".equalsIgnoreCase(assetType) ? "image" : "raw",
                    "type", type,
                    "invalidate", true
            ));
        } catch (Exception e) {
            throw new IllegalStateException("Cloudinary deletion failed", e);
        }
        Object result = response == null ? null : response.get("result");
        if (!"ok".equals(result) && !"not found".equals(result))
            throw new IllegalStateException("Cloudinary deletion incomplete");
    }

    private Cloudinary requireCloudinary() {
        Cloudinary cloudinary = cloudinaryProvider.getIfAvailable();
        if (cloudinary == null) {
            throw new IllegalStateException("Dịch vụ lưu trữ Cloudinary chưa được khởi tạo.");
        }
        return cloudinary;
    }
}
