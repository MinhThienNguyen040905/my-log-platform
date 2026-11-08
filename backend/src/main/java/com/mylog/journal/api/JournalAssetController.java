package com.mylog.journal.api;

import com.mylog.journal.api.request.BatchAssetUrlRequest;
import com.mylog.journal.api.response.BatchAssetUrlResponse;
import com.mylog.journal.api.response.JournalAssetUploadResponse;
import com.mylog.journal.api.response.JournalAssetUrlResponse;
import com.mylog.journal.application.JournalAssetService;
import com.mylog.journal.application.JournalAssetStore;
import com.mylog.journal.infrastructure.LocalFileAssetStorage;
import com.mylog.journal.infrastructure.persistence.entity.JournalAsset;
import com.mylog.platform.security.CurrentUserProvider;
import com.mylog.platform.web.InvalidRequestException;
import com.mylog.platform.web.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/journal-assets")
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class JournalAssetController {
    private final JournalAssetService assetService;
    private final JournalAssetStore assetStore;
    private final LocalFileAssetStorage localFileStorage;
    private final CurrentUserProvider current;

    public JournalAssetController(
            JournalAssetService assetService,
            JournalAssetStore assetStore,
            LocalFileAssetStorage localFileStorage,
            CurrentUserProvider current
    ) {
        this.assetService = assetService;
        this.assetStore = assetStore;
        this.localFileStorage = localFileStorage;
        this.current = current;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<JournalAssetUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        UUID userId = current.requireCurrent().userId();
        var uploaded = assetService.uploadDraftAsset(userId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(JournalAssetUploadResponse.from(uploaded));
    }

    @PostMapping("/urls")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<BatchAssetUrlResponse> batchUrls(@RequestBody BatchAssetUrlRequest request) {
        if (request == null || request.assetIds() == null) {
            throw new InvalidRequestException("Danh sách mã ảnh không được để trống.");
        }
        UUID userId = current.requireCurrent().userId();
        Map<UUID, String> urls = assetService.batchGetSignedUrls(userId, request.assetIds());
        return ResponseEntity.ok(new BatchAssetUrlResponse(urls));
    }

    @GetMapping("/{assetId}/url")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<JournalAssetUrlResponse> getUrl(@PathVariable UUID assetId) {
        UUID userId = current.requireCurrent().userId();
        String url = assetService.getSignedUrl(userId, assetId);
        return ResponseEntity.ok(new JournalAssetUrlResponse(url));
    }

    @GetMapping("/raw/{assetId}")
    public ResponseEntity<InputStreamResource> serveRaw(
            @PathVariable UUID assetId,
            @RequestParam("expires") long expires,
            @RequestParam("sig") String sig
    ) {
        if (!localFileStorage.verifyToken(assetId, expires, sig)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        JournalAsset asset = assetStore.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hình ảnh."));
        if (JournalAsset.STATUS_DELETED.equals(asset.status)) {
            return ResponseEntity.status(HttpStatus.GONE).build();
        }

        Path filePath = localFileStorage.resolveLocalFile(asset.userId, asset.id, asset.format);
        if (!Files.exists(filePath)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            InputStreamResource resource = new InputStreamResource(Files.newInputStream(filePath));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, asset.mimeType)
                    .header(HttpHeaders.CACHE_CONTROL, "private, max-age=900")
                    .header("X-Content-Type-Options", "nosniff")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + asset.id + "." + asset.format + "\"")
                    .contentLength(Files.size(filePath))
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

