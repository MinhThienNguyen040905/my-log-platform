package com.mylog.journal.application;

import com.mylog.journal.application.query.JournalTagView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface JournalTagStore {
    JournalTagView create(UUID userId, String name, String normalized, String color, Instant now);
    List<JournalTagView> list(UUID userId);
    boolean attach(UUID userId, UUID entryId, UUID tagId, Instant now);
    boolean detach(UUID userId, UUID entryId, UUID tagId);
}
