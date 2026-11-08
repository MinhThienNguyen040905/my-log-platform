package com.mylog.journal.api.response;

import java.util.Map;
import java.util.UUID;

public record BatchAssetUrlResponse(Map<UUID, String> urls) {}

