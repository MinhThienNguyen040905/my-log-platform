package com.mylog.knowledge.infrastructure;

import com.mylog.analysis.application.KnowledgeRetriever;
import com.mylog.knowledge.application.KnowledgeStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class ApprovedKnowledgeRetriever implements KnowledgeRetriever {
    private final KnowledgeStore store;
    private final Clock clock;
    ApprovedKnowledgeRetriever(KnowledgeStore store, Clock clock) { this.store=store; this.clock=clock; }
    @Transactional(readOnly = true)
    public List<Passage> retrieve(String topicCode, String locale, int limit) {
        if (topicCode == null || !topicCode.matches("[A-Z0-9_]{1,48}")
                || locale == null || !locale.matches("[a-z]{2}(?:-[A-Z]{2})?") || limit < 1 || limit > 10)
            throw new com.mylog.platform.web.InvalidRequestException();
        return store.retrieve(topicCode,locale,clock.instant(),limit).stream().map(p -> new Passage(
                p.itemId(),p.versionId(),p.version(),p.chunkId(),p.text(),p.sourceName(),p.sourceUrl())).toList();
    }
}
