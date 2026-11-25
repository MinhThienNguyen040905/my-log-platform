package com.mylog.journal.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Durable queue of files to remove from asset storage once their database rows are gone. */
public interface AssetDeletionQueue {
    /** Adds the files in the caller's transaction. */
    void enqueue(List<JournalAssetService.StoredFile> files, Instant now);

    /** Leases up to {@code limit} due items for 5 minutes and counts the attempt. */
    List<Item> claim(Instant now, int limit);

    void done(UUID id);

    void retry(UUID id, Instant availableAt, String errorCode);

    record Item(UUID id, String publicId, String assetType, String deliveryType, int attempt) {}
}
