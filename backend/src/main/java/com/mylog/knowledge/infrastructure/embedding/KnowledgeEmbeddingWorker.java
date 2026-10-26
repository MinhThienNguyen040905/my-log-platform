package com.mylog.knowledge.infrastructure.embedding;

import com.mylog.knowledge.application.EmbeddingProvider;
import com.mylog.knowledge.application.KnowledgeEmbeddingStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayList;

@Component
@ConditionalOnExpression("'${mylog.identity.enabled:false}' == 'true' && '${mylog.rag.embedding.enabled:false}' == 'true' && '${mylog.app-profile:all}' != 'api'")
class KnowledgeEmbeddingWorker {
    private final KnowledgeEmbeddingStore store;
    private final EmbeddingProvider provider;
    private final EmbeddingProperties properties;
    private final Clock clock;

    KnowledgeEmbeddingWorker(KnowledgeEmbeddingStore store, EmbeddingProvider provider,
                             EmbeddingProperties properties, Clock clock) {
        this.store = store;
        this.provider = provider;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${mylog.rag.embedding.poll-delay-ms:10000}")
    void poll() {
        var signature = new KnowledgeEmbeddingStore.ModelSignature("GEMINI", properties.model(),
                properties.modelVersion(), properties.dimensions());
        var pending = store.pending(signature, properties.batchSize());
        if (pending.isEmpty()) return;

        var inputs = pending.stream().map(chunk -> EmbeddingProvider.Input.document(chunk.title(), chunk.text())).toList();
        var batch = provider.embedDocuments(inputs);
        if (!signature.provider().equals(batch.provider()) || !signature.model().equals(batch.model())
                || !signature.modelVersion().equals(batch.modelVersion())
                || signature.dimensions() != batch.dimensions() || batch.vectors().size() != pending.size())
            throw new IllegalStateException("Embedding provider returned an incompatible batch");

        var embeddings = new ArrayList<KnowledgeEmbeddingStore.Embedding>(pending.size());
        for (int i = 0; i < pending.size(); i++)
            embeddings.add(new KnowledgeEmbeddingStore.Embedding(pending.get(i).chunkId(), batch.vectors().get(i)));
        store.save(signature, embeddings, clock.instant());
    }
}
