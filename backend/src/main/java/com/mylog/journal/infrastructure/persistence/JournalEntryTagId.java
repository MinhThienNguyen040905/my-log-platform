package com.mylog.journal.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class JournalEntryTagId implements Serializable {
    public UUID journalEntryId;
    public UUID tagId;

    public JournalEntryTagId() {}
    public JournalEntryTagId(UUID journalEntryId, UUID tagId) {
        this.journalEntryId = journalEntryId; this.tagId = tagId;
    }
    @Override public boolean equals(Object other) {
        return other instanceof JournalEntryTagId that && Objects.equals(journalEntryId, that.journalEntryId)
                && Objects.equals(tagId, that.tagId);
    }
    @Override public int hashCode() { return Objects.hash(journalEntryId, tagId); }
}
