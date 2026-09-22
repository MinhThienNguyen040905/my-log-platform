package com.mylog.journal.dto;

import java.util.List;

public record JournalPageResponse(
        List<JournalResponse> items,
        String nextCursor,
        boolean hasMore) {}
