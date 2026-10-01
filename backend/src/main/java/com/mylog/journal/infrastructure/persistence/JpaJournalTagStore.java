package com.mylog.journal.infrastructure.persistence;

import com.mylog.journal.application.JournalContentCipher;
import com.mylog.journal.application.JournalTagStore;
import com.mylog.journal.application.query.JournalTagView;
import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.platform.id.IdGenerator;
import com.mylog.platform.web.InvalidRequestException;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JpaJournalTagStore implements JournalTagStore {
    private final EntityManager em;
    private final IdGenerator ids;
    private final SensitiveDataCipher cipher;
    private final JournalContentCipher journalCipher;

    JpaJournalTagStore(EntityManager em, IdGenerator ids, SensitiveDataCipher cipher,
                       JournalContentCipher journalCipher) {
        this.em = em; this.ids = ids; this.cipher = cipher; this.journalCipher = journalCipher;
    }

    @Override public JournalTagView create(UUID userId, String name, String normalized, String color, Instant now) {
        byte[] hash = journalCipher.lookupHash(userId + ":" + normalized);
        var existing = em.createQuery("select t from JournalTag t where t.userId=:userId and t.nameLookupHash=:hash", JournalTag.class)
                .setParameter("userId", userId).setParameter("hash", hash).getResultStream().findFirst();
        if (existing.isPresent()) return view(existing.get());
        UUID id = ids.next();
        var encrypted = cipher.encrypt("journal_tags.name", userId, id, name);
        em.createNativeQuery("""
                insert into journal_tags(id,user_id,name_lookup_hash,encrypted_name,name_iv,name_wrapped_key,
                name_key_version,color,created_at,updated_at)
                values (?1,?2,?3,?4,?5,?6,?7,?8,?9,?9)
                on conflict (user_id,name_lookup_hash) do nothing
                """).setParameter(1, id).setParameter(2, userId).setParameter(3, hash)
                .setParameter(4, encrypted.ciphertext()).setParameter(5, encrypted.iv())
                .setParameter(6, encrypted.wrappedKey()).setParameter(7, encrypted.keyVersion())
                .setParameter(8, color).setParameter(9, now).executeUpdate();
        return em.createQuery("select t from JournalTag t where t.userId=:userId and t.nameLookupHash=:hash", JournalTag.class)
                .setParameter("userId", userId).setParameter("hash", hash).getResultStream().findFirst()
                .map(this::view).orElseThrow();
    }

    @Override public List<JournalTagView> list(UUID userId) {
        return em.createQuery("select t from JournalTag t where t.userId=:userId order by t.createdAt", JournalTag.class)
                .setParameter("userId", userId).getResultList().stream().map(this::view).toList();
    }

    @Override public boolean attach(UUID userId, UUID entryId, UUID tagId, Instant now) {
        if (!lockOwnedEntry(userId, entryId) || !ownsTag(userId, tagId)) return false;
        Number count = (Number) em.createNativeQuery("select count(*) from journal_entry_tags where journal_entry_id=?1")
                .setParameter(1, entryId).getSingleResult();
        Number attached = (Number) em.createNativeQuery("select count(*) from journal_entry_tags where journal_entry_id=?1 and tag_id=?2")
                .setParameter(1, entryId).setParameter(2, tagId).getSingleResult();
        if (attached.intValue() > 0) return true;
        if (count.intValue() >= 20) throw new InvalidRequestException();
        em.createNativeQuery("""
                insert into journal_entry_tags(journal_entry_id,tag_id,created_at) values (?1,?2,?3)
                on conflict (journal_entry_id,tag_id) do nothing
                """).setParameter(1, entryId).setParameter(2, tagId).setParameter(3, now).executeUpdate();
        return true;
    }

    @Override public boolean detach(UUID userId, UUID entryId, UUID tagId) {
        if (!owns(userId, entryId, tagId)) return false;
        em.createQuery("delete from JournalEntryTag jt where jt.journalEntryId=:entryId and jt.tagId=:tagId")
                .setParameter("entryId", entryId).setParameter("tagId", tagId).executeUpdate();
        return true;
    }

    private boolean owns(UUID userId, UUID entryId, UUID tagId) {
        Number count = (Number) em.createNativeQuery("""
                select count(*) from journal_entries j join journal_tags t on t.user_id=j.user_id
                where j.id=?1 and j.user_id=?2 and j.deleted_at is null and t.id=?3
                """).setParameter(1, entryId).setParameter(2, userId).setParameter(3, tagId).getSingleResult();
        return count.intValue() == 1;
    }

    private boolean lockOwnedEntry(UUID userId, UUID entryId) {
        return !em.createNativeQuery("""
                select j.id from journal_entries j where j.id=?1 and j.user_id=?2 and j.deleted_at is null
                for update
                """).setParameter(1, entryId).setParameter(2, userId).getResultList().isEmpty();
    }

    private boolean ownsTag(UUID userId, UUID tagId) {
        Number count = (Number) em.createNativeQuery("select count(*) from journal_tags where id=?1 and user_id=?2")
                .setParameter(1, tagId).setParameter(2, userId).getSingleResult();
        return count.intValue() == 1;
    }

    private JournalTagView view(JournalTag tag) {
        String name = cipher.decrypt("journal_tags.name", tag.userId, tag.id,
                new SensitiveDataCipher.Encrypted(tag.encryptedName, tag.nameIv,
                        tag.nameWrappedKey, tag.nameKeyVersion));
        return new JournalTagView(tag.id, name, tag.color);
    }
}
