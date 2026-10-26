package com.mylog.knowledge.infrastructure.persistence;

import com.mylog.knowledge.application.KnowledgeEmbeddingStore;
import com.mylog.platform.id.IdGenerator;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JpaKnowledgeEmbeddingStore implements KnowledgeEmbeddingStore {
    private final EntityManager em;
    private final IdGenerator ids;

    JpaKnowledgeEmbeddingStore(EntityManager em, IdGenerator ids) {
        this.em = em;
        this.ids = ids;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingChunk> pending(ModelSignature signature, int limit) {
        validate(signature);
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("Invalid embedding batch size");
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT c.id, v.title, c.content
                FROM knowledge_chunks c
                JOIN knowledge_versions v ON v.id=c.knowledge_version_id
                JOIN knowledge_items i ON i.id=v.item_id
                WHERE i.status='ACTIVE' AND v.status='APPROVED'
                  AND NOT EXISTS (
                    SELECT 1 FROM knowledge_embeddings e
                    WHERE e.chunk_id=c.id AND e.provider=:provider AND e.model=:model
                      AND e.model_version=:modelVersion AND e.dimensions=:dimensions
                  )
                ORDER BY v.approved_at, c.chunk_index
                LIMIT :limit
                """)
                .setParameter("provider", signature.provider()).setParameter("model", signature.model())
                .setParameter("modelVersion", signature.modelVersion()).setParameter("dimensions", signature.dimensions())
                .setParameter("limit", limit).getResultList();
        return rows.stream().map(row -> new PendingChunk((UUID) row[0], (String) row[1], (String) row[2])).toList();
    }

    @Override
    @Transactional
    public void save(ModelSignature signature, List<Embedding> embeddings, Instant now) {
        validate(signature);
        if (embeddings == null || embeddings.isEmpty()) throw new IllegalArgumentException("Embeddings are required");
        for (Embedding embedding : embeddings) {
            if (embedding == null || embedding.chunkId() == null || embedding.vector() == null
                    || embedding.vector().length != signature.dimensions())
                throw new IllegalArgumentException("Embedding dimensions do not match model signature");
            em.createNativeQuery("""
                    INSERT INTO knowledge_embeddings(id,chunk_id,provider,model,model_version,
                        dimensions,embedding,created_at)
                    VALUES (:id,:chunk,:provider,:model,:version,:dimensions,cast(:embedding as vector),:now)
                    ON CONFLICT (chunk_id,provider,model,model_version) DO NOTHING
                    """).setParameter("id", ids.next()).setParameter("chunk", embedding.chunkId())
                    .setParameter("provider", signature.provider()).setParameter("model", signature.model())
                    .setParameter("version", signature.modelVersion()).setParameter("dimensions", signature.dimensions())
                    .setParameter("embedding", vectorLiteral(embedding.vector())).setParameter("now", now)
                    .executeUpdate();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Match> search(float[] queryVector, ModelSignature signature, String topicCode,
                              String locale, Instant now, int limit) {
        validate(signature);
        if (queryVector == null || queryVector.length != signature.dimensions()
                || topicCode == null || !topicCode.matches("[A-Z0-9_]{1,48}")
                || locale == null || !locale.matches("[a-z]{2}(?:-[A-Z]{2})?")
                || limit < 1 || limit > 20) throw new IllegalArgumentException("Invalid embedding search");
        String vectorType = "vector(" + signature.dimensions() + ")";
        String sql = """
                SELECT i.id,v.id,v.version,c.id,c.content,i.source_name,i.source_url,
                    1 - ((e.embedding::%s) <=> cast(:queryVector as %s)) AS similarity
                FROM knowledge_embeddings e
                JOIN knowledge_chunks c ON c.id=e.chunk_id
                JOIN knowledge_versions v ON v.id=c.knowledge_version_id
                JOIN knowledge_items i ON i.id=v.item_id
                WHERE e.provider=:provider AND e.model=:model AND e.model_version=:modelVersion
                  AND e.dimensions=:dimensions AND i.topic_code=:topic AND i.locale=:locale
                  AND i.status='ACTIVE' AND v.status='APPROVED' AND v.approved_at<=:now
                  AND (v.effective_from IS NULL OR v.effective_from<=:now)
                  AND (v.effective_to IS NULL OR v.effective_to>:now)
                ORDER BY (e.embedding::%s) <=> cast(:queryVector as %s)
                LIMIT :limit
                """.formatted(vectorType, vectorType, vectorType, vectorType);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("queryVector", vectorLiteral(queryVector))
                .setParameter("provider", signature.provider()).setParameter("model", signature.model())
                .setParameter("modelVersion", signature.modelVersion()).setParameter("dimensions", signature.dimensions())
                .setParameter("topic", topicCode).setParameter("locale", locale).setParameter("now", now)
                .setParameter("limit", limit).getResultList();
        return rows.stream().map(row -> new Match((UUID) row[0], (UUID) row[1], ((Number) row[2]).intValue(),
                (UUID) row[3], (String) row[4], (String) row[5], (String) row[6],
                ((Number) row[7]).doubleValue())).toList();
    }

    static String vectorLiteral(float[] vector) {
        StringBuilder value = new StringBuilder(vector.length * 12).append('[');
        for (int i = 0; i < vector.length; i++) {
            if (!Float.isFinite(vector[i])) throw new IllegalArgumentException("Embedding contains non-finite value");
            if (i > 0) value.append(',');
            value.append(Float.toString(vector[i]));
        }
        return value.append(']').toString();
    }

    private static void validate(ModelSignature signature) {
        if (signature == null || signature.provider() == null || signature.provider().isBlank()
                || signature.model() == null || signature.model().isBlank()
                || signature.modelVersion() == null || signature.modelVersion().isBlank()
                || signature.dimensions() != 768 && signature.dimensions() != 1536)
            throw new IllegalArgumentException("Invalid embedding model signature");
    }
}
