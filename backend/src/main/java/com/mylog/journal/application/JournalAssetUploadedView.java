package com.mylog.journal.application;

import java.util.UUID;

public record JournalAssetUploadedView(
        UUID id,
        String format,
        String mimeType,
        int width,
        int height,
        long sizeBytes,
        String url
) {}

