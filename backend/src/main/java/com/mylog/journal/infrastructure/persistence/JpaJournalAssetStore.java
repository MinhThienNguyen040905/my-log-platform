package com.mylog.journal.infrastructure.persistence;

import com.mylog.journal.application.JournalAssetData;
import com.mylog.journal.application.JournalAssetService;
import com.mylog.journal.application.JournalAssetStore;
import com.mylog.journal.infrastructure.persistence.entity.JournalAsset;
import com.mylog.platform.web.ConflictException;
import com.mylog.platform.web.InvalidRequestException;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** JPA adapter for journal images; the entity stays here and callers get {@link JournalAssetData} copies. */
@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class JpaJournalAssetStore implements JournalAssetStore {
    private final EntityManager em;

    public JpaJournalAssetStore(EntityManager em) {
        this.em = em;
    }

    @Override
    public void save(JournalAssetData asset) {
        JournalAsset existing = asset.id == null ? null : em.find(JournalAsset.class, asset.id);
        if (existing != null) {
            copy(asset, existing);
        } else {
            JournalAsset created = new JournalAsset();
            copy(asset, created);
            em.persist(created);
        }
    }

    @Override
    public Optional<JournalAssetData> findById(UUID id) {
        return Optional.ofNullable(em.find(JournalAsset.class, id)).map(JpaJournalAssetStore::data);
    }

    @Override
    public Optional<JournalAssetData> findByUserIdAndId(UUID userId, UUID id) {
        List<JournalAsset> results = em.createQuery(
                "SELECT a FROM JournalAsset a WHERE a.userId = :userId AND a.id = :id", JournalAsset.class)
                .setParameter("userId", userId)
                .setParameter("id", id)
                .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(data(results.get(0)));
    }

    @Override
    public List<JournalAssetData> findAssetsByUserIdAndIds(UUID userId, Collection<UUID> ids) {
        return data(entitiesByUserIdAndIds(userId, ids));
    }

    @Override
    public List<JournalAssetService.StoredFile> findStoredFilesByUser(UUID userId) {
        return em.createQuery("SELECT a FROM JournalAsset a WHERE a.userId = :userId AND a.status <> :deleted", JournalAsset.class)
                .setParameter("userId", userId)
                .setParameter("deleted", JournalAssetData.STATUS_DELETED)
                .getResultList().stream()
                .map(a -> new JournalAssetService.StoredFile(a.publicId, a.assetType, a.deliveryType)).toList();
    }

    @Override
    public List<JournalAssetData> findAssetsForEntry(UUID userId, UUID entryId) {
        return data(em.createQuery(
                "SELECT a FROM JournalAsset a WHERE a.userId = :userId AND a.journalEntryId = :entryId AND a.status = :ready ORDER BY a.position ASC", JournalAsset.class)
                .setParameter("userId", userId)
                .setParameter("entryId", entryId)
                .setParameter("ready", JournalAssetData.STATUS_READY)
                .getResultList());
    }

    @Override
    public Map<UUID, JournalAssetData> findCoverAssetsForEntries(UUID userId, List<UUID> entryIds) {
        if (entryIds == null || entryIds.isEmpty()) return Map.of();
        List<JournalAsset> assets = em.createQuery(
                "SELECT a FROM JournalAsset a WHERE a.userId = :userId AND a.journalEntryId IN :entryIds AND a.status = :ready ORDER BY a.position ASC", JournalAsset.class)
                .setParameter("userId", userId)
                .setParameter("entryIds", entryIds)
                .setParameter("ready", JournalAssetData.STATUS_READY)
                .getResultList();

        Map<UUID, JournalAssetData> map = new HashMap<>();
        for (JournalAsset a : assets) {
            if (a.isCover) {
                map.put(a.journalEntryId, data(a));
            } else {
                map.putIfAbsent(a.journalEntryId, data(a));
            }
        }
        return map;
    }

    @Override
    public long sumActiveSizeBytesByUserId(UUID userId) {
        Long sum = em.createQuery(
                "SELECT COALESCE(SUM(a.sizeBytes), 0L) FROM JournalAsset a WHERE a.userId = :userId AND a.status <> :deleted", Long.class)
                .setParameter("userId", userId)
                .setParameter("deleted", JournalAssetData.STATUS_DELETED)
                .getSingleResult();
        return sum == null ? 0L : sum;
    }

    @Override
    public List<JournalAssetData> findOrphansBefore(Instant cutoff, int limit) {
        // Native query with FOR UPDATE SKIP LOCKED
        List<?> rows = em.createNativeQuery("""
                SELECT id FROM journal_assets
                WHERE status = 'PENDING_ATTACH' AND created_at < :cutoff
                ORDER BY created_at ASC
                LIMIT :limit
                FOR UPDATE SKIP LOCKED
                """)
                .setParameter("cutoff", cutoff)
                .setParameter("limit", limit)
                .getResultList();

        if (rows.isEmpty()) return List.of();
        List<UUID> ids = rows.stream().map(r -> (UUID) r).toList();
        return data(em.createQuery("SELECT a FROM JournalAsset a WHERE a.id IN :ids", JournalAsset.class)
                .setParameter("ids", ids)
                .getResultList());
    }

    @Override
    public void linkAssets(UUID userId, UUID entryId, List<UUID> assetIdsInOrder, Instant now) {
        // Works on managed entities: field changes are flushed with the surrounding transaction.
        List<JournalAsset> currentAssets = em.createQuery(
                "SELECT a FROM JournalAsset a WHERE a.userId = :userId AND a.journalEntryId = :entryId", JournalAsset.class)
                .setParameter("userId", userId)
                .setParameter("entryId", entryId)
                .getResultList();

        // Unlink assets that are no longer referenced
        for (JournalAsset a : currentAssets) {
            if (!assetIdsInOrder.contains(a.id)) {
                a.status = JournalAssetData.STATUS_DELETED;
                a.deletedAt = now;
            }
        }

        if (assetIdsInOrder.isEmpty()) return;

        Map<UUID, JournalAsset> requestedMap = new HashMap<>();
        for (JournalAsset a : entitiesByUserIdAndIds(userId, assetIdsInOrder)) {
            requestedMap.put(a.id, a);
        }

        for (int i = 0; i < assetIdsInOrder.size(); i++) {
            UUID assetId = assetIdsInOrder.get(i);
            JournalAsset asset = requestedMap.get(assetId);
            if (asset == null) {
                throw new InvalidRequestException("Hình ảnh đính kèm không tồn tại hoặc không thuộc quyền sở hữu của bạn.");
            }
            if (JournalAssetData.STATUS_DELETED.equals(asset.status)) {
                throw new ConflictException("Một số hình ảnh đính kèm đã hết hạn lưu tạm. Vui lòng tải lại ảnh.");
            }
            if (asset.journalEntryId != null && !asset.journalEntryId.equals(entryId)) {
                throw new InvalidRequestException("Hình ảnh đã được gắn vào một bài viết khác.");
            }
            asset.journalEntryId = entryId;
            asset.status = JournalAssetData.STATUS_READY;
            asset.position = i;
            asset.isCover = (i == 0);
        }
    }

    @Override
    public void softDeleteAssetsForEntry(UUID userId, UUID entryId, Instant now) {
        em.createQuery("""
                UPDATE JournalAsset a SET a.status = :deleted, a.deletedAt = :now
                WHERE a.userId = :userId AND a.journalEntryId = :entryId AND a.status <> :deleted
                """)
                .setParameter("deleted", JournalAssetData.STATUS_DELETED)
                .setParameter("now", now)
                .setParameter("userId", userId)
                .setParameter("entryId", entryId)
                .executeUpdate();
    }

    @Override
    public void markDeleted(UUID assetId, Instant now) {
        em.createQuery("UPDATE JournalAsset a SET a.status = :deleted, a.deletedAt = :now WHERE a.id = :id")
                .setParameter("deleted", JournalAssetData.STATUS_DELETED)
                .setParameter("now", now)
                .setParameter("id", assetId)
                .executeUpdate();
    }

    @Override
    public int purgeDeletedBefore(Instant cutoff) {
        return em.createQuery("DELETE FROM JournalAsset a WHERE a.status = :deleted AND a.deletedAt < :cutoff")
                .setParameter("deleted", JournalAssetData.STATUS_DELETED)
                .setParameter("cutoff", cutoff)
                .executeUpdate();
    }

    private List<JournalAsset> entitiesByUserIdAndIds(UUID userId, Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return em.createQuery(
                "SELECT a FROM JournalAsset a WHERE a.userId = :userId AND a.id IN :ids", JournalAsset.class)
                .setParameter("userId", userId)
                .setParameter("ids", ids)
                .getResultList();
    }

    private static List<JournalAssetData> data(List<JournalAsset> entities) {
        return entities.stream().map(JpaJournalAssetStore::data).toList();
    }

    private static JournalAssetData data(JournalAsset e) {
        JournalAssetData d = new JournalAssetData();
        d.id = e.id; d.journalEntryId = e.journalEntryId; d.userId = e.userId;
        d.providerAssetId = e.providerAssetId; d.publicId = e.publicId; d.providerVersion = e.providerVersion;
        d.format = e.format; d.deliveryType = e.deliveryType; d.assetType = e.assetType; d.mimeType = e.mimeType;
        d.sizeBytes = e.sizeBytes; d.sha256 = e.sha256; d.width = e.width; d.height = e.height;
        d.position = e.position; d.isCover = e.isCover; d.status = e.status;
        d.encryptedCaption = e.encryptedCaption; d.captionIv = e.captionIv; d.captionWrappedKey = e.captionWrappedKey;
        d.captionKeyVersion = e.captionKeyVersion; d.createdAt = e.createdAt; d.deletedAt = e.deletedAt;
        return d;
    }

    private static void copy(JournalAssetData d, JournalAsset e) {
        e.id = d.id; e.journalEntryId = d.journalEntryId; e.userId = d.userId;
        e.providerAssetId = d.providerAssetId; e.publicId = d.publicId; e.providerVersion = d.providerVersion;
        e.format = d.format; e.deliveryType = d.deliveryType; e.assetType = d.assetType; e.mimeType = d.mimeType;
        e.sizeBytes = d.sizeBytes; e.sha256 = d.sha256; e.width = d.width; e.height = d.height;
        e.position = d.position; e.isCover = d.isCover; e.status = d.status;
        e.encryptedCaption = d.encryptedCaption; e.captionIv = d.captionIv; e.captionWrappedKey = d.captionWrappedKey;
        e.captionKeyVersion = d.captionKeyVersion; e.createdAt = d.createdAt; e.deletedAt = d.deletedAt;
    }
}
