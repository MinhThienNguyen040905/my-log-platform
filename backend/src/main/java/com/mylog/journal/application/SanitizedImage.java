package com.mylog.journal.application;

public record SanitizedImage(
        byte[] bytes,
        String format,
        String mimeType,
        int width,
        int height,
        byte[] sha256
) {}

