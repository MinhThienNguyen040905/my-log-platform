package com.mylog.knowledge.application;

import com.mylog.knowledge.application.query.KnowledgeVersionView;
import com.mylog.knowledge.domain.KnowledgeChunker;
import com.mylog.platform.id.IdGenerator;
import com.mylog.platform.web.ConflictException;
import com.mylog.platform.web.InvalidRequestException;
import com.mylog.platform.web.ResourceNotFoundException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class KnowledgeService {
    private final KnowledgeStore store;
    private final IdGenerator ids;
    private final Clock clock;
    public KnowledgeService(KnowledgeStore store, IdGenerator ids, Clock clock) {
        this.store = store; this.ids = ids; this.clock = clock;
    }

    @Transactional
    public KnowledgeVersionView create(UUID actor, String slug, String topicCode, String locale,
                                       String sourceName, String sourceUrl, String ownerTeam,
                                       String title, String content, Instant effectiveFrom, Instant effectiveTo) {
        if (slug == null || !slug.matches("[a-z0-9]+(?:-[a-z0-9]+)*") || slug.length() > 160
                || topicCode == null || !topicCode.matches("[A-Z0-9_]{1,48}")
                || locale == null || !locale.matches("[a-z]{2}(?:-[A-Z]{2})?")
                || sourceName == null || sourceName.isBlank() || sourceName.length() > 240
                || ownerTeam == null || ownerTeam.isBlank() || ownerTeam.length() > 80
                || sourceUrl != null && (!sourceUrl.startsWith("https://") || sourceUrl.length() > 2000))
            throw new InvalidRequestException();
        validate(title, content, effectiveFrom, effectiveTo);
        Instant now = clock.instant(); UUID itemId = ids.next(); UUID versionId = ids.next();
        var item = new KnowledgeStore.Item(itemId, slug, topicCode, locale, sourceName, sourceUrl,
                ownerTeam, "ACTIVE", actor, now, now);
        var version = new KnowledgeStore.Version(versionId, itemId, 1, title.strip(), content.strip(),
                sha256(content.strip()), "DRAFT", null, KnowledgeChunker.VERSION, actor, null, null,
                null, effectiveFrom, effectiveTo, now, now);
        store.create(item, version);
        return view(item, version);
    }

    @Transactional(readOnly = true)
    public KnowledgeVersionView get(UUID itemId, int number) {
        var item = requireItem(itemId);
        return view(item, requireVersion(itemId, number));
    }

    @Transactional
    public KnowledgeVersionView revise(UUID actor, UUID itemId, String title, String content,
                                       Instant effectiveFrom, Instant effectiveTo) {
        var item = requireItem(itemId);
        validate(title, content, effectiveFrom, effectiveTo);
        Instant now = clock.instant();
        var version = new KnowledgeStore.Version(ids.next(), itemId, store.latestVersion(itemId) + 1,
                title.strip(), content.strip(), sha256(content.strip()), "DRAFT", null,
                KnowledgeChunker.VERSION, actor, null, null, null, effectiveFrom, effectiveTo, now, now);
        store.addVersion(version);
        return view(item, version);
    }

    @Transactional
    public KnowledgeVersionView updateDraft(UUID actor, UUID itemId, int number, String title, String content) {
        var item = requireItem(itemId); var old = requireLockedVersion(itemId, number);
        if (!"DRAFT".equals(old.status())) throw new ConflictException("Version không còn ở trạng thái draft.");
        validate(title, content, old.effectiveFrom(), old.effectiveTo());
        byte[] hash = sha256(content.strip());
        if (old.title().equals(title.strip()) && MessageDigest.isEqual(old.checksum(), hash)) return view(item, old);
        var next = new KnowledgeStore.Version(old.id(), itemId, number, title.strip(), content.strip(), hash,
                old.status(), null, old.chunkStrategyVersion(), old.createdBy(), null, null, null,
                old.effectiveFrom(), old.effectiveTo(), old.createdAt(), clock.instant());
        store.replaceChunks(old.id(), List.of());
        store.updateVersion(next);
        return view(item, next);
    }

    @Transactional
    public KnowledgeVersionView submit(UUID actor, UUID itemId, int number) {
        var item = requireItem(itemId); var old = requireLockedVersion(itemId, number);
        if (!"DRAFT".equals(old.status())) throw new ConflictException("Version không còn ở trạng thái draft.");
        Instant now = clock.instant();
        var parts = KnowledgeChunker.chunks(old.content());
        var chunks = new java.util.ArrayList<KnowledgeStore.Chunk>();
        for (int i = 0; i < parts.size(); i++)
            chunks.add(new KnowledgeStore.Chunk(ids.next(), old.id(), i, parts.get(i),
                    Math.max(1, parts.get(i).split("\\s+").length), now));
        store.replaceChunks(old.id(), chunks);
        var next = copy(old, "IN_REVIEW", null, null, null, now);
        store.updateVersion(next);
        return view(item, next);
    }

    @Transactional
    public KnowledgeVersionView review(UUID actor, UUID itemId, int number, boolean approve, String reasonCode) {
        var item = requireItem(itemId); var old = requireLockedVersion(itemId, number);
        if (!"IN_REVIEW".equals(old.status()) || actor.equals(old.createdBy()))
            throw new ConflictException("Version không thể được review bởi actor này.");
        if (!approve && (reasonCode == null || !reasonCode.matches("[A-Z0-9_]{3,60}")))
            throw new InvalidRequestException();
        Instant now = clock.instant();
        var next = new KnowledgeStore.Version(old.id(), itemId, number, old.title(), old.content(), old.checksum(),
                approve ? "APPROVED" : "REJECTED", approve ? null : reasonCode,
                old.chunkStrategyVersion(), old.createdBy(), actor, approve ? actor : null,
                approve ? now : null, old.effectiveFrom(), old.effectiveTo(), old.createdAt(), now);
        store.updateVersion(next);
        if (approve) store.audit(actor, "KNOWLEDGE_APPROVED", itemId, null, now);
        else store.audit(actor, "KNOWLEDGE_REJECTED", itemId, reasonCode, now);
        return view(item, next);
    }

    @Transactional
    public KnowledgeVersionView archive(UUID actor, UUID itemId, int number, String reasonCode) {
        var item = requireItem(itemId); var old = requireLockedVersion(itemId, number);
        if (!"APPROVED".equals(old.status()) || reasonCode == null
                || !reasonCode.matches("[A-Z0-9_]{3,60}")) throw new InvalidRequestException();
        Instant now = clock.instant();
        var next = copy(old, "ARCHIVED", reasonCode, old.reviewedBy(), old.approvedBy(), now);
        store.updateVersion(next);
        store.audit(actor, "KNOWLEDGE_ARCHIVED", itemId, reasonCode, now);
        return view(item, next);
    }

    private KnowledgeStore.Item requireItem(UUID id) {
        return store.item(id).orElseThrow(() -> new ResourceNotFoundException("Knowledge item không tồn tại."));
    }
    private KnowledgeStore.Version requireVersion(UUID itemId, int number) {
        return store.version(itemId, number).orElseThrow(() -> new ResourceNotFoundException("Knowledge version không tồn tại."));
    }
    private KnowledgeStore.Version requireLockedVersion(UUID itemId, int number) {
        return store.lockedVersion(itemId, number)
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge version không tồn tại."));
    }
    private static KnowledgeStore.Version copy(KnowledgeStore.Version old, String status, String notes,
                                                UUID reviewer, UUID approver, Instant now) {
        return new KnowledgeStore.Version(old.id(), old.itemId(), old.number(), old.title(), old.content(),
                old.checksum(), status, notes, old.chunkStrategyVersion(), old.createdBy(), reviewer,
                approver, old.approvedAt(), old.effectiveFrom(), old.effectiveTo(), old.createdAt(), now);
    }
    private static void validate(String title, String content, Instant from, Instant to) {
        if (title == null || title.isBlank() || title.length() > 300 || content == null
                || content.isBlank() || content.length() > 40000 || from != null && to != null && !to.isAfter(from))
            throw new InvalidRequestException();
    }
    private static byte[] sha256(String content) {
        try { return MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static KnowledgeVersionView view(KnowledgeStore.Item item, KnowledgeStore.Version version) {
        return new KnowledgeVersionView(item.id(), version.id(), version.number(), item.slug(), item.topicCode(),
                item.locale(), version.title(), version.content(), version.status(),
                HexFormat.of().formatHex(version.checksum()), version.chunkStrategyVersion(),
                version.effectiveFrom(), version.effectiveTo());
    }
}
