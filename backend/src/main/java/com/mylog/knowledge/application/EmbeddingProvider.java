package com.mylog.knowledge.application;

import java.util.List;

/** Creates document and query vectors in the same embedding space. */
public interface EmbeddingProvider {
    enum TaskType { RETRIEVAL_DOCUMENT, RETRIEVAL_QUERY }

    record Input(String text, String title) {
        public static Input document(String title, String text) {
            return new Input(text, title);
        }

        public static Input query(String text) {
            return new Input(text, null);
        }
    }

    record Batch(String provider, String model, String modelVersion, int dimensions,
                 List<float[]> vectors) {}

    Batch embed(List<Input> inputs, TaskType taskType);

    default Batch embedDocuments(List<Input> documents) {
        return embed(documents, TaskType.RETRIEVAL_DOCUMENT);
    }

    default float[] embedQuery(String query) {
        return embed(List.of(Input.query(query)), TaskType.RETRIEVAL_QUERY).vectors().getFirst();
    }
}
