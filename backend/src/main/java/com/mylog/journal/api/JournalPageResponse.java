package com.mylog.journal.api;

import java.util.List;

public record JournalPageResponse(
        List<JournalResponse> items,
        String nextCursor,
        boolean hasMore) {}
