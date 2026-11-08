package com.mylog.journal.infrastructure;

import com.mylog.journal.application.AssetStoredResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileAssetStorageTest {
    @TempDir
    Path tempDir;

    private LocalFileAssetStorage storage;
    private final String secret = "test-secret-key-1234567890-test-key";

    @BeforeEach
    void setUp() {
        storage = new LocalFileAssetStorage(tempDir.toString(), secret);
    }

    @Test
    void storesFileSafelyUsingUuidNaming() {
        UUID userId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        byte[] bytes = new byte[]{1, 2, 3, 4};

        AssetStoredResult result = storage.store(userId, assetId, bytes, "jpg", "image/jpeg");
        assertNotNull(result);
        assertEquals(assetId.toString(), result.providerAssetId());
        assertEquals(userId + "/" + assetId, result.publicId());

        Path savedFile = storage.resolveLocalFile(userId, assetId, "jpg");
        assertTrue(Files.exists(savedFile));
    }

    @Test
    void verifiesHmacTokenCorrectlyAndRejectsTampered() {
        UUID assetId = UUID.randomUUID();
        long nowSeconds = System.currentTimeMillis() / 1000L;
        long expires = nowSeconds + 3600;

        String signedUrl = storage.generateSignedUrl(UUID.randomUUID() + "/" + assetId, "jpg", "AUTHENTICATED", Duration.ofHours(1));
        assertTrue(signedUrl.contains("sig="));
        assertTrue(signedUrl.contains("expires="));

        // Extract expires and sig from URL
        String[] parts = signedUrl.split("\\?");
        assertTrue(parts.length > 1);
        String[] queryParams = parts[1].split("&");
        long urlExpires = 0;
        String urlSig = "";
        for (String param : queryParams) {
            String[] kv = param.split("=");
            if ("expires".equals(kv[0])) urlExpires = Long.parseLong(kv[1]);
            if ("sig".equals(kv[0])) urlSig = kv[1];
        }

        assertTrue(storage.verifyToken(assetId, urlExpires, urlSig));
        assertFalse(storage.verifyToken(assetId, urlExpires, urlSig + "tampered"));
        assertFalse(storage.verifyToken(UUID.randomUUID(), urlExpires, urlSig));
        assertFalse(storage.verifyToken(assetId, nowSeconds - 100, urlSig)); // expired
    }

    @Test
    void deletesFileFromDisk() {
        UUID userId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        byte[] bytes = new byte[]{1, 2, 3, 4};

        storage.store(userId, assetId, bytes, "png", "image/png");
        Path savedFile = storage.resolveLocalFile(userId, assetId, "png");
        assertTrue(Files.exists(savedFile));

        storage.delete(userId + "/" + assetId, "IMAGE", "AUTHENTICATED");
        assertFalse(Files.exists(savedFile));
    }
}

