package com.mylog.knowledge.infrastructure.persistence;

import com.mylog.knowledge.application.KnowledgeStore;
import com.mylog.platform.id.IdGenerator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JpaKnowledgeStore implements KnowledgeStore {
    private final EntityManager em;
    private final IdGenerator ids;
    JpaKnowledgeStore(EntityManager em, IdGenerator ids) { this.em = em; this.ids = ids; }

    public void create(Item item, Version version) {
        KnowledgeItem row = new KnowledgeItem();
        row.id=item.id(); row.slug=item.slug(); row.topicCode=item.topicCode(); row.locale=item.locale();
        row.sourceName=item.sourceName(); row.sourceUrl=item.sourceUrl(); row.ownerTeam=item.ownerTeam();
        row.status=item.status(); row.createdBy=item.createdBy(); row.createdAt=item.createdAt();
        row.updatedAt=item.updatedAt(); em.persist(row);
        addVersion(version);
    }
    public Optional<Item> item(UUID id) {
        return Optional.ofNullable(em.find(KnowledgeItem.class, id)).map(this::item);
    }
    public Optional<Version> version(UUID itemId, int number) {
        return em.createQuery("select v from KnowledgeVersion v where v.itemId=:item and v.version=:number", KnowledgeVersion.class)
                .setParameter("item", itemId).setParameter("number", number)
                .getResultStream().findFirst().map(this::version);
    }
    public Optional<Version> lockedVersion(UUID itemId, int number) {
        return em.createQuery("select v from KnowledgeVersion v where v.itemId=:item and v.version=:number", KnowledgeVersion.class)
                .setParameter("item",itemId).setParameter("number",number)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream().findFirst().map(this::version);
    }
    public int latestVersion(UUID itemId) {
        em.find(KnowledgeItem.class,itemId,LockModeType.PESSIMISTIC_WRITE);
        return em.createQuery("select coalesce(max(v.version),0) from KnowledgeVersion v where v.itemId=:item", Integer.class)
                .setParameter("item", itemId).getSingleResult();
    }
    public void addVersion(Version version) {
        KnowledgeVersion row = new KnowledgeVersion();
        copy(row, version); em.persist(row);
    }
    public void updateVersion(Version version) {
        KnowledgeVersion row = em.find(KnowledgeVersion.class, version.id());
        copy(row, version); em.flush();
    }
    public void replaceChunks(UUID versionId, List<Chunk> chunks) {
        em.createQuery("delete from KnowledgeChunk c where c.knowledgeVersionId=:version")
                .setParameter("version", versionId).executeUpdate();
        for (Chunk chunk : chunks) {
            KnowledgeChunk row = new KnowledgeChunk();
            row.id=chunk.id(); row.knowledgeVersionId=versionId; row.chunkIndex=chunk.index();
            row.content=chunk.content(); row.tokenCount=chunk.tokenCount(); row.metadata=Map.of("strategy","paragraph-800-v1");
            row.createdAt=chunk.createdAt(); em.persist(row);
        }
        em.flush();
    }
    public List<Passage> retrieve(String topicCode, String locale, Instant now, int limit) {
        List<Object[]> rows = em.createQuery("""
                select c,v,i from KnowledgeChunk c, KnowledgeVersion v, KnowledgeItem i
                where c.knowledgeVersionId=v.id and v.itemId=i.id
                  and i.topicCode=:topic and i.locale=:locale and i.status='ACTIVE'
                  and v.status='APPROVED' and v.approvedAt<=:now
                  and (v.effectiveFrom is null or v.effectiveFrom<=:now)
                  and (v.effectiveTo is null or v.effectiveTo>:now)
                order by v.approvedAt desc, c.chunkIndex asc
                """, Object[].class).setParameter("topic", topicCode).setParameter("locale", locale)
                .setParameter("now", now).setMaxResults(limit).getResultList();
        return rows.stream().map(row -> {
            KnowledgeChunk chunk=(KnowledgeChunk) row[0]; KnowledgeVersion version=(KnowledgeVersion) row[1];
            KnowledgeItem item=(KnowledgeItem) row[2];
            return new Passage(item.id, version.id, version.version, chunk.id, chunk.content,
                    item.sourceName, item.sourceUrl);
        }).toList();
    }
    public void audit(UUID actor, String action, UUID itemId, String reasonCode, Instant now) {
        em.createNativeQuery("""
                insert into audit_logs(id,actor_user_id,actor_type,action,target_type,target_id,reason_code,occurred_at)
                values (:id,:actor,'ADMIN',:action,'KNOWLEDGE_ITEM',:target,:reason,:now)
                """).setParameter("id", ids.next()).setParameter("actor", actor)
                .setParameter("action", action).setParameter("target", itemId)
                .setParameter("reason", reasonCode).setParameter("now", now).executeUpdate();
    }
    private Item item(KnowledgeItem r) {
        return new Item(r.id,r.slug,r.topicCode,r.locale,r.sourceName,r.sourceUrl,r.ownerTeam,r.status,
                r.createdBy,r.createdAt,r.updatedAt);
    }
    private Version version(KnowledgeVersion r) {
        return new Version(r.id,r.itemId,r.version,r.title,r.content,r.contentSha256,r.status,r.reviewNotes,
                r.chunkStrategyVersion,r.createdBy,r.reviewedBy,r.approvedBy,r.approvedAt,r.effectiveFrom,
                r.effectiveTo,r.createdAt,r.updatedAt);
    }
    private static void copy(KnowledgeVersion r, Version v) {
        r.id=v.id(); r.itemId=v.itemId(); r.version=v.number(); r.title=v.title(); r.content=v.content();
        r.contentSha256=v.checksum(); r.status=v.status(); r.reviewNotes=v.reviewNotes();
        r.chunkStrategyVersion=v.chunkStrategyVersion(); r.createdBy=v.createdBy(); r.reviewedBy=v.reviewedBy();
        r.approvedBy=v.approvedBy(); r.approvedAt=v.approvedAt(); r.effectiveFrom=v.effectiveFrom();
        r.effectiveTo=v.effectiveTo(); r.createdAt=v.createdAt(); r.updatedAt=v.updatedAt();
    }
}
