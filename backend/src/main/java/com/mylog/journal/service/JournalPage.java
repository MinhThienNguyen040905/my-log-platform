package com.mylog.journal.service;

import com.mylog.journal.entity.JournalEntry;
import java.util.List;

public record JournalPage(
        List<JournalEntry> items,
        String nextCursor,
        boolean hasMore) {}
