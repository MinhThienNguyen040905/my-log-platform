package com.mylog.journal.application;

import com.mylog.journal.application.query.JournalTagView;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface JournalTagStore {
    JournalTagView create(UUID userId, String name, String normalized, String color, Instant now);
    List<JournalTagView> list(UUID userId);
    List<Link> links(UUID userId);
    record Link(UUID journalEntryId, UUID tagId) {}
    boolean attach(UUID userId, UUID entryId, UUID tagId, Instant now);
    boolean detach(UUID userId, UUID entryId, UUID tagId);
    Map<UUID, List<JournalTagView>> listTagsForEntries(UUID userId, Collection<UUID> entryIds);
    List<JournalTagView> listTagsForEntry(UUID userId, UUID entryId);
    void replaceTags(UUID userId, UUID entryId, List<UUID> tagIds, Instant now);
    default boolean ownsEntry(UUID userId, UUID entryId) { return true; }
}
