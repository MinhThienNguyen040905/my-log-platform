package com.mylog.journal.infrastructure.persistence;

import com.mylog.journal.application.AssetDeletionQueue;
import com.mylog.journal.application.JournalAssetService;
import com.mylog.platform.id.IdGenerator;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JpaAssetDeletionQueue implements AssetDeletionQueue {
    private final EntityManager em;
    private final IdGenerator ids;

    JpaAssetDeletionQueue(EntityManager em, IdGenerator ids) { this.em = em; this.ids = ids; }

    @Override public void enqueue(List<JournalAssetService.StoredFile> files, Instant now) {
        for (JournalAssetService.StoredFile file : files) {
            em.createNativeQuery("""
                    INSERT INTO asset_deletion_queue(id,public_id,asset_type,delivery_type,attempt,available_at,created_at)
                    VALUES (:id,:publicId,:assetType,:deliveryType,0,:now,:now)
                    """).setParameter("id", ids.next()).setParameter("publicId", file.publicId())
                    .setParameter("assetType", file.assetType()).setParameter("deliveryType", file.deliveryType())
                    .setParameter("now", Timestamp.from(now)).executeUpdate();
        }
    }

    @Override public List<Item> claim(Instant now, int limit) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                UPDATE asset_deletion_queue SET attempt = attempt + 1, lease_expires_at = :lease
                WHERE id IN (
                    SELECT id FROM asset_deletion_queue
                    WHERE available_at <= :now AND (lease_expires_at IS NULL OR lease_expires_at < :now)
                    ORDER BY available_at LIMIT :limit FOR UPDATE SKIP LOCKED)
                RETURNING id, public_id, asset_type, delivery_type, attempt
                """).setParameter("now", Timestamp.from(now)).setParameter("lease", Timestamp.from(now.plusSeconds(300)))
                .setParameter("limit", limit).getResultList();
        return rows.stream().map(row -> new Item((UUID) row[0], (String) row[1], (String) row[2], (String) row[3],
                ((Number) row[4]).intValue())).toList();
    }

    @Override public void done(UUID id) {
        em.createNativeQuery("DELETE FROM asset_deletion_queue WHERE id = :id").setParameter("id", id).executeUpdate();
    }

    @Override public void retry(UUID id, Instant availableAt, String errorCode) {
        em.createNativeQuery("""
                UPDATE asset_deletion_queue SET available_at = :at, lease_expires_at = NULL, last_error_code = :code
                WHERE id = :id
                """).setParameter("at", Timestamp.from(availableAt)).setParameter("code", errorCode)
                .setParameter("id", id).executeUpdate();
    }
}
