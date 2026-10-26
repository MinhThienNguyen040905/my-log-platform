package com.mylog.knowledge.infrastructure;

import com.mylog.analysis.application.KnowledgeRetriever;
import com.mylog.knowledge.application.KnowledgeStore;
import com.mylog.knowledge.application.EmbeddingProvider;
import com.mylog.knowledge.application.KnowledgeEmbeddingStore;
import com.mylog.knowledge.infrastructure.embedding.EmbeddingProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class ApprovedKnowledgeRetriever implements KnowledgeRetriever {
    private final KnowledgeStore store;
    private final KnowledgeEmbeddingStore embeddings;
    private final EmbeddingProvider provider;
    private final EmbeddingProperties properties;
    private final Clock clock;
    ApprovedKnowledgeRetriever(KnowledgeStore store, KnowledgeEmbeddingStore embeddings,
                               EmbeddingProvider provider, EmbeddingProperties properties, Clock clock) {
        this.store=store; this.embeddings=embeddings; this.provider=provider;
        this.properties=properties; this.clock=clock;
    }
    public List<Passage> retrieve(String topicCode, String locale, int limit) {
        if (topicCode == null || !topicCode.matches("[A-Z0-9_]{1,48}")
                || locale == null || !locale.matches("[a-z]{2}(?:-[A-Z]{2})?") || limit < 1 || limit > 10)
            throw new com.mylog.platform.web.InvalidRequestException();
        var now = clock.instant();
        if (properties.enabled()) {
            try {
                var signature = new KnowledgeEmbeddingStore.ModelSignature("GEMINI", properties.model(),
                        properties.modelVersion(), properties.dimensions());
                float[] query = provider.embedQuery("recommendations for topic " + topicCode);
                var matches = embeddings.search(query, signature, topicCode, locale, now, limit);
                if (!matches.isEmpty()) return matches.stream().map(p -> new Passage(p.itemId(), p.versionId(),
                        p.version(), p.chunkId(), p.text(), p.sourceName(), p.sourceUrl())).toList();
            } catch (RuntimeException ignored) {
                // Retrieval remains available from the approved knowledge catalog if the provider is unavailable.
            }
        }
        return store.retrieve(topicCode,locale,now,limit).stream().map(p -> new Passage(
                p.itemId(),p.versionId(),p.version(),p.chunkId(),p.text(),p.sourceName(),p.sourceUrl())).toList();
    }
}
