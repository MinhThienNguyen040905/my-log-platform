package com.mylog.journal.api.request;

import java.util.List;
import java.util.UUID;

public record BatchAssetUrlRequest(List<UUID> assetIds) {}

