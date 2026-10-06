package com.mylog.export.api.response;
import com.mylog.export.application.ExportService;
import java.time.Instant;
public record DownloadGrantResponse(String url, Instant expiresAt) {
    public static DownloadGrantResponse from(ExportService.DownloadGrant grant) {
        return new DownloadGrantResponse(grant.url(),grant.expiresAt());
    }
}
