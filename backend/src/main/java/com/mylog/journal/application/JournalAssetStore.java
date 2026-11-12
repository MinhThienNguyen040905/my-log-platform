package com.mylog.journal.application;

import com.mylog.journal.infrastructure.persistence.entity.JournalAsset;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface JournalAssetStore {
    void save(JournalAsset asset);
    Optional<JournalAsset> findById(UUID id);
    Optional<JournalAsset> findByUserIdAndId(UUID userId, UUID id);
    List<JournalAsset> findAssetsByUserIdAndIds(UUID userId, Collection<UUID> ids);
    List<JournalAsset> findAssetsForEntry(UUID userId, UUID entryId);
    /** Files of every asset of the user that may still be in storage (anything not yet DELETED). */
    List<JournalAssetService.StoredFile> findStoredFilesByUser(UUID userId);
    Map<UUID, JournalAsset> findCoverAssetsForEntries(UUID userId, List<UUID> entryIds);
    long sumActiveSizeBytesByUserId(UUID userId);
    List<JournalAsset> findOrphansBefore(Instant cutoff, int limit);
    void linkAssets(UUID userId, UUID entryId, List<UUID> assetIdsInOrder, Instant now);
    void softDeleteAssetsForEntry(UUID userId, UUID entryId, Instant now);
    void markDeleted(UUID assetId, Instant now);
    int purgeDeletedBefore(Instant cutoff);
}

