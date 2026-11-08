package com.mylog.journal.application;

public record AssetStoredResult(
        String providerAssetId,
        String publicId,
        long providerVersion,
        String deliveryType
) {}

