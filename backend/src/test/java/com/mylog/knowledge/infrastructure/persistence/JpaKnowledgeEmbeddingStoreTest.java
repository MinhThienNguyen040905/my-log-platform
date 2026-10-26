package com.mylog.knowledge.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JpaKnowledgeEmbeddingStoreTest {
    @Test void serializesFiniteVectorForPgvector() {
        assertEquals("[0.5,-0.25,0.0]",
                JpaKnowledgeEmbeddingStore.vectorLiteral(new float[]{0.5f, -0.25f, 0f}));
    }

    @Test void rejectsNonFiniteVector() {
        assertThrows(IllegalArgumentException.class,
                () -> JpaKnowledgeEmbeddingStore.vectorLiteral(new float[]{Float.NaN}));
    }
}
