package com.mylog.knowledge.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KnowledgeStore {
    record Item(UUID id, String slug, String topicCode, String locale, String sourceName,
                String sourceUrl, String ownerTeam, String status, UUID createdBy, Instant createdAt, Instant updatedAt) {}
    record Version(UUID id, UUID itemId, int number, String title, String content, byte[] checksum,
                   String status, String reviewNotes, String chunkStrategyVersion, UUID createdBy,
                   UUID reviewedBy, UUID approvedBy, Instant approvedAt, Instant effectiveFrom,
                   Instant effectiveTo, Instant createdAt, Instant updatedAt) {}
    record Chunk(UUID id, UUID versionId, int index, String content, int tokenCount, Instant createdAt) {}
    record Passage(UUID itemId, UUID versionId, int version, UUID chunkId, String text,
                   String sourceName, String sourceUrl) {}

    void create(Item item, Version version);
    Optional<Item> item(UUID id);
    Optional<Version> version(UUID itemId, int number);
    Optional<Version> lockedVersion(UUID itemId, int number);
    int latestVersion(UUID itemId);
    void addVersion(Version version);
    void updateVersion(Version version);
    void replaceChunks(UUID versionId, List<Chunk> chunks);
    List<Passage> retrieve(String topicCode, String locale, Instant now, int limit);
    void audit(UUID actor, String action, UUID itemId, String reasonCode, Instant now);
}
