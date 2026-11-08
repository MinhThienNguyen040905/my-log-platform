package com.mylog.journal.api.response;

import com.mylog.journal.application.JournalAssetUploadedView;

import java.util.UUID;

public record JournalAssetUploadResponse(
        UUID id,
        String format,
        String mimeType,
        int width,
        int height,
        long sizeBytes,
        String url
) {
    public static JournalAssetUploadResponse from(JournalAssetUploadedView view) {
        return new JournalAssetUploadResponse(
                view.id(),
                view.format(),
                view.mimeType(),
                view.width(),
                view.height(),
                view.sizeBytes(),
                view.url()
        );
    }
}

