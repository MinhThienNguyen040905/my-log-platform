package com.mylog.journal.infrastructure;

import com.mylog.journal.application.AssetStoredResult;
import com.mylog.journal.application.LocalAssetFiles;
import com.mylog.journal.application.JournalAssetStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

@Component
public class LocalFileAssetStorage implements JournalAssetStorage, LocalAssetFiles {
    private final Path rootPath;
    private final byte[] secretKey;

    public LocalFileAssetStorage(
            @Value("${mylog.assets.local-dir:data/assets}") String localDir,
            @Value("${mylog.assets.local-raw-secret:dev-asset-secret-key-must-be-changed-in-prod}") String secret
    ) {
        this.rootPath = Paths.get(localDir).toAbsolutePath().normalize();
        this.secretKey = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Override
    public AssetStoredResult store(UUID userId, UUID assetId, byte[] cleanBytes, String format, String mimeType) {
        Path userDir = rootPath.resolve(userId.toString());
        try {
            Files.createDirectories(userDir);
            String filename = assetId.toString() + "." + format;
            Path targetFile = userDir.resolve(filename).normalize();
            if (!targetFile.startsWith(userDir)) {
                throw new SecurityException("Phát hiện đường dẫn không hợp lệ.");
            }
            Files.write(targetFile, cleanBytes);
            String publicId = userId + "/" + assetId;
            return new AssetStoredResult(assetId.toString(), publicId, 1L, "AUTHENTICATED");
        } catch (IOException e) {
            throw new IllegalStateException("Không thể lưu tệp ảnh cục bộ.", e);
        }
    }

    @Override
    public String generateSignedUrl(String publicId, String format, String deliveryType, Duration ttl) {
        return buildSignedLocalUrl(publicId, ttl);
    }

    @Override
    public String generateThumbnailUrl(String publicId, String format, String deliveryType, int width, int height, Duration ttl) {
        return buildSignedLocalUrl(publicId, ttl);
    }

    @Override
    public void delete(String publicId, String assetType, String deliveryType) {
        if (publicId == null || !publicId.contains("/")) return;
        String[] parts = publicId.split("/", 2);
        Path userDir = rootPath.resolve(parts[0]);
        File dir = userDir.toFile();
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.startsWith(parts[1] + "."));
            if (files != null) {
                for (File f : files) {
                    //noinspection ResultOfMethodCallIgnored
                    f.delete();
                }
            }
        }
    }

    @Override
    public boolean verifyToken(UUID assetId, long expires, String sig) {
        if (sig == null || sig.isBlank()) return false;
        long nowSeconds = System.currentTimeMillis() / 1000L;
        if (expires < nowSeconds) return false;
        String expectedSig = calculateSig(assetId.toString(), expires);
        return MessageDigest.isEqual(
                expectedSig.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                sig.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
    }

    @Override
    public Path resolveLocalFile(UUID userId, UUID assetId, String format) {
        Path userDir = rootPath.resolve(userId.toString());
        return userDir.resolve(assetId.toString() + "." + format).normalize();
    }

    private String buildSignedLocalUrl(String publicId, Duration ttl) {
        if (publicId == null || !publicId.contains("/")) return "";
        String[] parts = publicId.split("/", 2);
        String assetId = parts[1];
        long roundedExpires = ((System.currentTimeMillis() / 1000L) + ttl.toSeconds()) / 900L * 900L;
        String sig = calculateSig(assetId, roundedExpires);
        return "/api/v1/journal-assets/raw/" + assetId + "?expires=" + roundedExpires + "&sig=" + sig;
    }

    private String calculateSig(String assetId, long expires) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey, "HmacSHA256"));
            byte[] raw = mac.doFinal((assetId + ":" + expires).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Lỗi khi tạo chữ ký HMAC.", e);
        }
    }
}

