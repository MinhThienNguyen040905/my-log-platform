package com.mylog.knowledge.infrastructure.embedding;

import com.mylog.knowledge.application.EmbeddingProvider;

import java.util.List;

final class UnavailableEmbeddingProvider implements EmbeddingProvider {
    @Override
    public Batch embed(List<Input> inputs, TaskType taskType) {
        throw new IllegalStateException("RAG embedding is disabled");
    }
}
