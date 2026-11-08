package com.mylog.journal.infrastructure.persistence;

import com.mylog.journal.application.JournalAssetStore;
import com.mylog.journal.infrastructure.persistence.entity.JournalAsset;
import com.mylog.platform.web.ConflictException;
import com.mylog.platform.web.InvalidRequestException;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaJournalAssetStore implements JournalAssetStore {
    private final EntityManager em;

    public JpaJournalAssetStore(EntityManager em) {
        this.em = em;
    }

    @Override
    public void save(JournalAsset asset) {
        if (asset.id != null && em.find(JournalAsset.class, asset.id) != null) {
            em.merge(asset);
        } else {
            em.persist(asset);
        }
    }

    @Override
    public Optional<JournalAsset> findById(UUID id) {
        return Optional.ofNullable(em.find(JournalAsset.class, id));
    }

    @Override
    public Optional<JournalAsset> findByUserIdAndId(UUID userId, UUID id) {
        List<JournalAsset> results = em.createQuery(
                "SELECT a FROM JournalAsset a WHERE a.userId = :userId AND a.id = :id", JournalAsset.class)
                .setParameter("userId", userId)
                .setParameter("id", id)
                .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<JournalAsset> findAssetsByUserIdAndIds(UUID userId, Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return em.createQuery(
                "SELECT a FROM JournalAsset a WHERE a.userId = :userId AND a.id IN :ids", JournalAsset.class)
                .setParameter("userId", userId)
                .setParameter("ids", ids)
                .getResultList();
    }

    @Override
    public List<JournalAsset> findAssetsForEntry(UUID userId, UUID entryId) {
        return em.createQuery(
                "SELECT a FROM JournalAsset a WHERE a.userId = :userId AND a.journalEntryId = :entryId AND a.status = :ready ORDER BY a.position ASC", JournalAsset.class)
                .setParameter("userId", userId)
                .setParameter("entryId", entryId)
                .setParameter("ready", JournalAsset.STATUS_READY)
                .getResultList();
    }

    @Override
    public Map<UUID, JournalAsset> findCoverAssetsForEntries(UUID userId, List<UUID> entryIds) {
        if (entryIds == null || entryIds.isEmpty()) return Map.of();
        List<JournalAsset> assets = em.createQuery(
                "SELECT a FROM JournalAsset a WHERE a.userId = :userId AND a.journalEntryId IN :entryIds AND a.status = :ready ORDER BY a.position ASC", JournalAsset.class)
                .setParameter("userId", userId)
                .setParameter("entryIds", entryIds)
                .setParameter("ready", JournalAsset.STATUS_READY)
                .getResultList();

        Map<UUID, JournalAsset> map = new HashMap<>();
        for (JournalAsset a : assets) {
            if (a.isCover) {
                map.put(a.journalEntryId, a);
            } else {
                map.putIfAbsent(a.journalEntryId, a);
            }
        }
        return map;
    }

    @Override
    public long sumActiveSizeBytesByUserId(UUID userId) {
        Long sum = em.createQuery(
                "SELECT COALESCE(SUM(a.sizeBytes), 0L) FROM JournalAsset a WHERE a.userId = :userId AND a.status <> :deleted", Long.class)
                .setParameter("userId", userId)
                .setParameter("deleted", JournalAsset.STATUS_DELETED)
                .getSingleResult();
        return sum == null ? 0L : sum;
    }

    @Override
    public List<JournalAsset> findOrphansBefore(Instant cutoff, int limit) {
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
        return em.createQuery("SELECT a FROM JournalAsset a WHERE a.id IN :ids", JournalAsset.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    @Override
    public void linkAssets(UUID userId, UUID entryId, List<UUID> assetIdsInOrder, Instant now) {
        List<JournalAsset> currentAssets = em.createQuery(
                "SELECT a FROM JournalAsset a WHERE a.userId = :userId AND a.journalEntryId = :entryId", JournalAsset.class)
                .setParameter("userId", userId)
                .setParameter("entryId", entryId)
                .getResultList();

        Map<UUID, JournalAsset> currentMap = new HashMap<>();
        for (JournalAsset a : currentAssets) {
            currentMap.put(a.id, a);
        }

        // Unlink assets that are no longer referenced
        for (JournalAsset a : currentAssets) {
            if (!assetIdsInOrder.contains(a.id)) {
                a.status = JournalAsset.STATUS_DELETED;
                a.deletedAt = now;
            }
        }

        if (assetIdsInOrder.isEmpty()) return;

        // Fetch all requested assets
        List<JournalAsset> requestedAssets = findAssetsByUserIdAndIds(userId, assetIdsInOrder);
        Map<UUID, JournalAsset> requestedMap = new HashMap<>();
        for (JournalAsset a : requestedAssets) {
            requestedMap.put(a.id, a);
        }

        for (int i = 0; i < assetIdsInOrder.size(); i++) {
            UUID assetId = assetIdsInOrder.get(i);
            JournalAsset asset = requestedMap.get(assetId);
            if (asset == null) {
                throw new InvalidRequestException("Hình ảnh đính kèm không tồn tại hoặc không thuộc quyền sở hữu của bạn.");
            }
            if (JournalAsset.STATUS_DELETED.equals(asset.status)) {
                throw new ConflictException("Một số hình ảnh đính kèm đã hết hạn lưu tạm. Vui lòng tải lại ảnh.");
            }
            if (asset.journalEntryId != null && !asset.journalEntryId.equals(entryId)) {
                throw new InvalidRequestException("Hình ảnh đã được gắn vào một bài viết khác.");
            }
            asset.journalEntryId = entryId;
            asset.status = JournalAsset.STATUS_READY;
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
                .setParameter("deleted", JournalAsset.STATUS_DELETED)
                .setParameter("now", now)
                .setParameter("userId", userId)
                .setParameter("entryId", entryId)
                .executeUpdate();
    }

    @Override
    public void markDeleted(UUID assetId, Instant now) {
        em.createQuery("UPDATE JournalAsset a SET a.status = :deleted, a.deletedAt = :now WHERE a.id = :id")
                .setParameter("deleted", JournalAsset.STATUS_DELETED)
                .setParameter("now", now)
                .setParameter("id", assetId)
                .executeUpdate();
    }

    @Override
    public int purgeDeletedBefore(Instant cutoff) {
        return em.createQuery("DELETE FROM JournalAsset a WHERE a.status = :deleted AND a.deletedAt < :cutoff")
                .setParameter("deleted", JournalAsset.STATUS_DELETED)
                .setParameter("cutoff", cutoff)
                .executeUpdate();
    }
}

