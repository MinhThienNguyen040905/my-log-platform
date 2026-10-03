package com.mylog.journal.infrastructure.persistence;

import com.mylog.journal.infrastructure.persistence.entity.JournalEntry;

import com.mylog.journal.application.JournalEntrySnapshot;
import com.mylog.journal.application.JournalStore;
import com.mylog.platform.crypto.SensitiveDataCipher;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JpaJournalStore implements JournalStore {
    private final EntityManager entityManager;

    JpaJournalStore(EntityManager entityManager) { this.entityManager = entityManager; }

    @Override public void create(JournalEntrySnapshot snapshot) {
        JournalEntry entry = new JournalEntry();
        entry.id = snapshot.id();
        entry.userId = snapshot.userId();
        apply(entry, snapshot);
        entry.contentFormatVersion = 1;
        entry.createdAt = snapshot.createdAt();
        entityManager.persist(entry);
        entityManager.flush();
    }

    @Override public Optional<JournalEntrySnapshot> find(UUID userId, UUID entryId) {
        return entityManager.createQuery("""
                select j from JournalEntry j where j.id=:entryId and j.userId=:userId and j.deletedAt is null
                """, JournalEntry.class).setParameter("entryId", entryId).setParameter("userId", userId)
                .getResultStream().findFirst().map(this::snapshot);
    }

    @Override public List<JournalEntrySnapshot> list(UUID userId, LocalDate from, LocalDate to, UUID tagId,
                                                      Boolean favorite, Instant cursorTime, UUID cursorId, int limit) {
        StringBuilder sql = new StringBuilder("SELECT j.* FROM journal_entries j WHERE j.user_id=?1 AND j.deleted_at IS NULL");
        int next = 2;
        if (from != null) sql.append(" AND j.local_date >= ?").append(next++);
        if (to != null) sql.append(" AND j.local_date <= ?").append(next++);
        if (favorite != null) sql.append(" AND j.favorite = ?").append(next++);
        if (tagId != null) sql.append(" AND EXISTS (SELECT 1 FROM journal_entry_tags jt JOIN journal_tags t ON t.id=jt.tag_id")
                .append(" WHERE jt.journal_entry_id=j.id AND t.user_id=?1 AND t.id=?").append(next++).append(")");
        if (cursorTime != null) sql.append(" AND (j.occurred_at,j.id) < (?").append(next++)
                .append(",?").append(next++).append(")");
        sql.append(" ORDER BY j.occurred_at DESC,j.id DESC LIMIT ?").append(next);
        Query query = entityManager.createNativeQuery(sql.toString(), JournalEntry.class);
        int index = 1;
        query.setParameter(index++, userId);
        if (from != null) query.setParameter(index++, from);
        if (to != null) query.setParameter(index++, to);
        if (favorite != null) query.setParameter(index++, favorite);
        if (tagId != null) query.setParameter(index++, tagId);
        if (cursorTime != null) {
            query.setParameter(index++, cursorTime);
            query.setParameter(index++, cursorId);
        }
        query.setParameter(index, limit);
        @SuppressWarnings("unchecked")
        List<JournalEntry> entries = query.getResultList();
        return entries.stream().map(this::snapshot).toList();
    }

    @Override public List<JournalEntrySnapshot> exportAll(UUID userId, int limit) {
        return entityManager.createQuery("""
                select j from JournalEntry j where j.userId=:user order by j.occurredAt desc, j.id desc
                """,JournalEntry.class).setParameter("user",userId).setMaxResults(limit)
                .getResultList().stream().map(this::snapshot).toList();
    }

    @Override public boolean update(JournalEntrySnapshot entry, long expectedVersion) {
        int changed = entityManager.createQuery("""
                update JournalEntry j set j.encryptedPayload=:ciphertext, j.payloadIv=:iv,
                    j.wrappedDataKey=:wrappedKey, j.encryptionKeyVersion=:keyVersion,
                    j.occurredAt=:occurredAt, j.localDate=:localDate, j.timezone=:timezone,
                    j.moodCode=:moodCode, j.moodScore=:moodScore, j.stressScore=:stressScore,
                    j.energyScore=:energyScore, j.sleepMinutes=:sleepMinutes,
                    j.riskLevel=:riskLevel, j.analysisStatus=:analysisStatus,
                    j.contentVersion=:contentVersion, j.latestAnalysisId=null, j.updatedAt=:updatedAt,
                    j.rowVersion=j.rowVersion+1
                where j.id=:id and j.userId=:userId and j.deletedAt is null and j.rowVersion=:expectedVersion
                """)
                .setParameter("ciphertext", entry.payload().ciphertext())
                .setParameter("iv", entry.payload().iv())
                .setParameter("wrappedKey", entry.payload().wrappedKey())
                .setParameter("keyVersion", entry.payload().keyVersion())
                .setParameter("occurredAt", entry.occurredAt())
                .setParameter("localDate", entry.localDate())
                .setParameter("timezone", entry.timezone())
                .setParameter("moodCode", entry.moodCode())
                .setParameter("moodScore", entry.moodScore())
                .setParameter("stressScore", entry.stressScore())
                .setParameter("energyScore", entry.energyScore())
                .setParameter("sleepMinutes", entry.sleepMinutes() == null ? null : entry.sleepMinutes().shortValue())
                .setParameter("riskLevel", entry.riskLevel())
                .setParameter("analysisStatus", entry.analysisStatus())
                .setParameter("contentVersion", entry.contentVersion())
                .setParameter("updatedAt", entry.updatedAt())
                .setParameter("id", entry.id())
                .setParameter("userId", entry.userId())
                .setParameter("expectedVersion", expectedVersion)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
        return changed == 1;
    }

    @Override public boolean setFavorite(UUID userId, UUID entryId, boolean favorite, Instant now) {
        int changed = entityManager.createQuery("""
                update JournalEntry j set j.favorite=:favorite,
                    j.rowVersion=case when j.favorite=:favorite then j.rowVersion else j.rowVersion+1 end,
                    j.updatedAt=case when j.favorite=:favorite then j.updatedAt else :now end
                where j.id=:entryId and j.userId=:userId and j.deletedAt is null
                """).setParameter("favorite", favorite).setParameter("now", now)
                .setParameter("entryId", entryId).setParameter("userId", userId).executeUpdate();
        entityManager.flush();
        entityManager.clear();
        return changed == 1;
    }

    @Override public boolean softDelete(UUID userId, UUID entryId, long expectedVersion, Instant now) {
        int changed = entityManager.createQuery("""
                update JournalEntry j set j.deletedAt=:now, j.entryStatus='DELETED',
                    j.analysisStatus='NOT_REQUESTED', j.updatedAt=:now, j.rowVersion=j.rowVersion+1
                where j.id=:entryId and j.userId=:userId and j.deletedAt is null and j.rowVersion=:expectedVersion
                """).setParameter("now", now).setParameter("entryId", entryId)
                .setParameter("userId", userId).setParameter("expectedVersion", expectedVersion).executeUpdate();
        entityManager.flush();
        entityManager.clear();
        return changed == 1;
    }

    @Override public boolean transitionAnalysis(UUID userId, UUID entryId, int contentVersion,
                                                List<String> from, String to, UUID analysisId, Instant now) {
        int changed = entityManager.createQuery("""
                update JournalEntry j set j.analysisStatus=:to, j.latestAnalysisId=:analysisId,
                    j.updatedAt=:now, j.rowVersion=j.rowVersion+1
                where j.id=:entryId and j.userId=:userId and j.deletedAt is null
                  and j.contentVersion=:contentVersion and j.analysisStatus in :from
                """).setParameter("to", to).setParameter("analysisId", analysisId)
                .setParameter("now", now).setParameter("entryId", entryId).setParameter("userId", userId)
                .setParameter("contentVersion", contentVersion).setParameter("from", from).executeUpdate();
        entityManager.clear();
        return changed == 1;
    }

    @Override public boolean applyRescreen(UUID userId, UUID entryId, int contentVersion,
                                           String riskLevel, String analysisStatus, Instant now) {
        int changed = entityManager.createQuery("""
                update JournalEntry j set j.riskLevel=:riskLevel, j.analysisStatus=:analysisStatus,
                    j.updatedAt=:now, j.rowVersion=j.rowVersion+1
                where j.id=:entryId and j.userId=:userId and j.deletedAt is null
                    and j.contentVersion=:contentVersion and j.analysisStatus='BLOCKED_BY_SAFETY'
                """).setParameter("riskLevel", riskLevel).setParameter("analysisStatus", analysisStatus)
                .setParameter("now", now).setParameter("entryId", entryId).setParameter("userId", userId)
                .setParameter("contentVersion", contentVersion).executeUpdate();
        entityManager.clear();
        return changed == 1;
    }

    @Override public int purgeDeletedBefore(Instant cutoff) {
        return entityManager.createNativeQuery("""
                delete from journal_entries j where j.deleted_at < ?1
                  and not exists (select 1 from journal_assets a where a.journal_entry_id=j.id and a.status <> 'DELETED')
                """).setParameter(1, cutoff).executeUpdate();
    }

    private static void apply(JournalEntry entry, JournalEntrySnapshot snapshot) {
        entry.encryptedPayload = snapshot.payload().ciphertext();
        entry.payloadIv = snapshot.payload().iv();
        entry.wrappedDataKey = snapshot.payload().wrappedKey();
        entry.encryptionKeyVersion = snapshot.payload().keyVersion();
        entry.occurredAt = snapshot.occurredAt();
        entry.localDate = snapshot.localDate();
        entry.timezone = snapshot.timezone();
        entry.moodCode = snapshot.moodCode();
        entry.moodScore = snapshot.moodScore();
        entry.stressScore = snapshot.stressScore();
        entry.energyScore = snapshot.energyScore();
        entry.sleepMinutes = snapshot.sleepMinutes() == null ? null : snapshot.sleepMinutes().shortValue();
        entry.favorite = snapshot.favorite();
        entry.entryStatus = snapshot.entryStatus();
        entry.riskLevel = snapshot.riskLevel();
        entry.analysisStatus = snapshot.analysisStatus();
        entry.contentVersion = snapshot.contentVersion();
        entry.updatedAt = snapshot.updatedAt();
        entry.deletedAt = snapshot.deletedAt();
        entry.rowVersion = snapshot.rowVersion();
    }

    private JournalEntrySnapshot snapshot(JournalEntry entry) {
        return new JournalEntrySnapshot(entry.id, entry.userId,
                new SensitiveDataCipher.Encrypted(entry.encryptedPayload, entry.payloadIv,
                        entry.wrappedDataKey, entry.encryptionKeyVersion), entry.occurredAt,
                entry.localDate, entry.timezone, entry.moodCode, entry.moodScore, entry.stressScore,
                entry.energyScore, entry.sleepMinutes == null ? null : entry.sleepMinutes.intValue(),
                entry.favorite, entry.entryStatus, entry.riskLevel,
                entry.analysisStatus, entry.contentVersion, entry.rowVersion, entry.createdAt,
                entry.updatedAt, entry.deletedAt);
    }
}
