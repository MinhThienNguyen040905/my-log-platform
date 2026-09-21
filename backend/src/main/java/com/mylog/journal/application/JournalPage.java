package com.mylog.journal.application;

import com.mylog.journal.domain.JournalEntry;
import java.util.List;

public record JournalPage(
        List<JournalEntry> items,
        String nextCursor,
        boolean hasMore) {}
