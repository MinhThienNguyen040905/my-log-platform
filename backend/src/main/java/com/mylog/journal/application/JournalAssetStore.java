package com.mylog.journal.application;


import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface JournalAssetStore {
    void save(JournalAssetData asset);
    Optional<JournalAssetData> findById(UUID id);
    Optional<JournalAssetData> findByUserIdAndId(UUID userId, UUID id);
    List<JournalAssetData> findAssetsByUserIdAndIds(UUID userId, Collection<UUID> ids);
    List<JournalAssetData> findAssetsForEntry(UUID userId, UUID entryId);
    /** Files of every asset of the user that may still be in storage (anything not yet DELETED). */
    List<JournalAssetService.StoredFile> findStoredFilesByUser(UUID userId);
    Map<UUID, JournalAssetData> findCoverAssetsForEntries(UUID userId, List<UUID> entryIds);
    long sumActiveSizeBytesByUserId(UUID userId);
    List<JournalAssetData> findOrphansBefore(Instant cutoff, int limit);
    void linkAssets(UUID userId, UUID entryId, List<UUID> assetIdsInOrder, Instant now);
    void softDeleteAssetsForEntry(UUID userId, UUID entryId, Instant now);
    void markDeleted(UUID assetId, Instant now);
    int purgeDeletedBefore(Instant cutoff);
}

