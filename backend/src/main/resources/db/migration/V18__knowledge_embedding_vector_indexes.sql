-- The embedding column intentionally remains unbounded so model provenance can coexist.
-- Expression indexes provide fixed dimensions required by pgvector HNSW indexes.
CREATE INDEX idx_knowledge_embeddings_chunk_model
    ON knowledge_embeddings(chunk_id, provider, model, model_version);

CREATE INDEX idx_knowledge_embeddings_hnsw_cosine_768
    ON knowledge_embeddings
    USING hnsw ((embedding::vector(768)) vector_cosine_ops)
    WHERE dimensions = 768;

CREATE INDEX idx_knowledge_embeddings_hnsw_cosine_1536
    ON knowledge_embeddings
    USING hnsw ((embedding::vector(1536)) vector_cosine_ops)
    WHERE dimensions = 1536;
