CREATE TABLE knowledge_items (
    id UUID PRIMARY KEY,
    slug VARCHAR(160) NOT NULL UNIQUE,
    topic_code VARCHAR(48) NOT NULL,
    locale VARCHAR(10) NOT NULL,
    source_name VARCHAR(240) NOT NULL,
    source_url TEXT,
    owner_team VARCHAR(80) NOT NULL,
    status VARCHAR(24) NOT NULL CHECK (status IN ('ACTIVE','ARCHIVED')),
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CHECK (source_url IS NULL OR source_url LIKE 'https://%')
);
CREATE INDEX idx_knowledge_items_topic_locale ON knowledge_items(topic_code, locale, status);

CREATE TABLE knowledge_versions (
    id UUID PRIMARY KEY,
    item_id UUID NOT NULL REFERENCES knowledge_items(id) ON DELETE CASCADE,
    version INTEGER NOT NULL CHECK (version > 0),
    title VARCHAR(300) NOT NULL,
    content TEXT NOT NULL,
    content_sha256 BYTEA NOT NULL CHECK (octet_length(content_sha256) = 32),
    status VARCHAR(24) NOT NULL CHECK (status IN ('DRAFT','IN_REVIEW','APPROVED','REJECTED','ARCHIVED')),
    review_notes TEXT,
    chunk_strategy_version VARCHAR(40) NOT NULL,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    reviewed_by UUID REFERENCES users(id) ON DELETE SET NULL,
    approved_by UUID REFERENCES users(id) ON DELETE SET NULL,
    approved_at TIMESTAMPTZ,
    effective_from TIMESTAMPTZ,
    effective_to TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE(item_id, version),
    CHECK (effective_to IS NULL OR effective_from IS NULL OR effective_to > effective_from),
    CHECK (status NOT IN ('APPROVED','ARCHIVED') OR approved_at IS NOT NULL)
);
CREATE INDEX idx_knowledge_versions_live ON knowledge_versions(item_id, status, effective_from, effective_to);
CREATE UNIQUE INDEX uq_knowledge_item_approved ON knowledge_versions(item_id) WHERE status = 'APPROVED';

CREATE TABLE knowledge_chunks (
    id UUID PRIMARY KEY,
    knowledge_version_id UUID NOT NULL REFERENCES knowledge_versions(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL CHECK (chunk_index >= 0),
    content TEXT NOT NULL,
    token_count INTEGER NOT NULL CHECK (token_count > 0),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE(knowledge_version_id, chunk_index)
);
CREATE INDEX idx_knowledge_chunks_version ON knowledge_chunks(knowledge_version_id, chunk_index);

CREATE TABLE knowledge_embeddings (
    id UUID PRIMARY KEY,
    chunk_id UUID NOT NULL REFERENCES knowledge_chunks(id) ON DELETE CASCADE,
    provider VARCHAR(40) NOT NULL,
    model VARCHAR(120) NOT NULL,
    model_version VARCHAR(120) NOT NULL,
    dimensions INTEGER NOT NULL CHECK (dimensions BETWEEN 1 AND 2000),
    embedding VECTOR NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE(chunk_id, provider, model, model_version),
    CHECK (vector_dims(embedding) = dimensions)
);

CREATE TABLE journal_prompts (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    locale VARCHAR(10) NOT NULL,
    category VARCHAR(48) NOT NULL,
    prompt_text TEXT NOT NULL,
    status VARCHAR(24) NOT NULL CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED')),
    display_order INTEGER NOT NULL DEFAULT 0,
    valid_from TIMESTAMPTZ,
    valid_to TIMESTAMPTZ,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    updated_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CHECK (valid_to IS NULL OR valid_from IS NULL OR valid_to > valid_from)
);
CREATE INDEX idx_journal_prompts_published ON journal_prompts(locale, display_order)
    WHERE status = 'PUBLISHED';

CREATE FUNCTION guard_approved_knowledge() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' AND OLD.status IN ('APPROVED','ARCHIVED') THEN
        RAISE EXCEPTION 'approved knowledge version is immutable';
    END IF;
    IF TG_OP = 'UPDATE' AND OLD.status IN ('APPROVED','ARCHIVED') THEN
        IF NEW.item_id IS DISTINCT FROM OLD.item_id OR NEW.version IS DISTINCT FROM OLD.version
            OR NEW.title IS DISTINCT FROM OLD.title OR NEW.content IS DISTINCT FROM OLD.content
            OR NEW.content_sha256 IS DISTINCT FROM OLD.content_sha256
            OR NEW.chunk_strategy_version IS DISTINCT FROM OLD.chunk_strategy_version
            OR NEW.created_by IS DISTINCT FROM OLD.created_by
            OR NEW.reviewed_by IS DISTINCT FROM OLD.reviewed_by
            OR NEW.approved_by IS DISTINCT FROM OLD.approved_by
            OR NEW.approved_at IS DISTINCT FROM OLD.approved_at
            OR NEW.created_at IS DISTINCT FROM OLD.created_at
            OR NEW.effective_from IS DISTINCT FROM OLD.effective_from
            OR NEW.effective_to IS DISTINCT FROM OLD.effective_to
            OR (OLD.status = 'ARCHIVED' AND NEW.status <> 'ARCHIVED')
            OR NEW.status NOT IN ('APPROVED','ARCHIVED') THEN
            RAISE EXCEPTION 'approved knowledge version is immutable';
        END IF;
    END IF;
    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END $$;
CREATE TRIGGER trg_knowledge_version_immutable BEFORE UPDATE OR DELETE ON knowledge_versions
    FOR EACH ROW EXECUTE FUNCTION guard_approved_knowledge();

CREATE FUNCTION guard_approved_chunk() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'INSERT' AND EXISTS (SELECT 1 FROM knowledge_versions
            WHERE id = NEW.knowledge_version_id AND status IN ('APPROVED','ARCHIVED')) THEN
        RAISE EXCEPTION 'approved knowledge chunk is immutable';
    END IF;
    IF TG_OP <> 'INSERT' AND EXISTS (SELECT 1 FROM knowledge_versions
            WHERE id = OLD.knowledge_version_id AND status IN ('APPROVED','ARCHIVED')) THEN
        RAISE EXCEPTION 'approved knowledge chunk is immutable';
    END IF;
    IF TG_OP = 'UPDATE' AND EXISTS (SELECT 1 FROM knowledge_versions
            WHERE id = NEW.knowledge_version_id AND status IN ('APPROVED','ARCHIVED')) THEN
        RAISE EXCEPTION 'approved knowledge chunk is immutable';
    END IF;
    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END $$;
CREATE TRIGGER trg_knowledge_chunk_immutable BEFORE INSERT OR UPDATE OR DELETE ON knowledge_chunks
    FOR EACH ROW EXECUTE FUNCTION guard_approved_chunk();

CREATE FUNCTION guard_approved_item() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM knowledge_versions WHERE item_id=OLD.id
               AND status IN ('APPROVED','ARCHIVED')) THEN
        IF TG_OP='DELETE' THEN RAISE EXCEPTION 'approved knowledge item is immutable'; END IF;
        IF NEW.slug IS DISTINCT FROM OLD.slug OR NEW.topic_code IS DISTINCT FROM OLD.topic_code
            OR NEW.locale IS DISTINCT FROM OLD.locale OR NEW.source_name IS DISTINCT FROM OLD.source_name
            OR NEW.source_url IS DISTINCT FROM OLD.source_url OR NEW.owner_team IS DISTINCT FROM OLD.owner_team THEN
            RAISE EXCEPTION 'approved knowledge item is immutable';
        END IF;
    END IF;
    RETURN CASE WHEN TG_OP='DELETE' THEN OLD ELSE NEW END;
END $$;
CREATE TRIGGER trg_knowledge_item_immutable BEFORE UPDATE OR DELETE ON knowledge_items
    FOR EACH ROW EXECUTE FUNCTION guard_approved_item();
