CREATE TABLE journal_analyses (
    id UUID PRIMARY KEY,
    journal_entry_id UUID NOT NULL REFERENCES journal_entries(id) ON DELETE CASCADE,
    journal_version BIGINT NOT NULL,
    sentiment VARCHAR(20) NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    summary TEXT,
    explanation TEXT,
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(30) NOT NULL,
    schema_version VARCHAR(30) NOT NULL,
    input_token_count INTEGER,
    output_token_count INTEGER,
    latency_ms BIGINT,
    is_current BOOLEAN NOT NULL DEFAULT TRUE,
    analyzed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_journal_analysis_version UNIQUE (journal_entry_id, journal_version),
    CONSTRAINT ck_analysis_sentiment CHECK (sentiment IN ('POSITIVE', 'NEUTRAL', 'NEGATIVE')),
    CONSTRAINT ck_analysis_risk CHECK (risk_level IN ('NORMAL', 'LOW', 'MODERATE', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_analysis_tokens CHECK (
        (input_token_count IS NULL OR input_token_count >= 0)
        AND (output_token_count IS NULL OR output_token_count >= 0)
    ),
    CONSTRAINT ck_analysis_latency CHECK (latency_ms IS NULL OR latency_ms >= 0)
);

CREATE UNIQUE INDEX uq_journal_analysis_current
    ON journal_analyses (journal_entry_id) WHERE is_current = TRUE;
CREATE INDEX idx_journal_analysis_version
    ON journal_analyses (journal_entry_id, journal_version DESC);

CREATE TABLE journal_emotions (
    id UUID PRIMARY KEY,
    analysis_id UUID NOT NULL REFERENCES journal_analyses(id) ON DELETE CASCADE,
    emotion_type VARCHAR(30) NOT NULL,
    original_score NUMERIC(5,4) NOT NULL,
    corrected_score NUMERIC(5,4),
    corrected_by_user BOOLEAN NOT NULL DEFAULT FALSE,
    corrected_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_journal_emotion_type UNIQUE (analysis_id, emotion_type),
    CONSTRAINT ck_emotion_type CHECK (emotion_type IN (
        'JOY', 'SADNESS', 'ANGER', 'FEAR', 'ANXIETY', 'CALM',
        'HOPE', 'GRATITUDE', 'LONELINESS', 'FRUSTRATION', 'EXCITEMENT'
    )),
    CONSTRAINT ck_emotion_original_score CHECK (original_score BETWEEN 0 AND 1),
    CONSTRAINT ck_emotion_corrected_score CHECK (corrected_score IS NULL OR corrected_score BETWEEN 0 AND 1),
    CONSTRAINT ck_emotion_correction_consistency CHECK (
        (corrected_by_user = FALSE AND corrected_score IS NULL AND corrected_at IS NULL)
        OR (corrected_by_user = TRUE AND corrected_score IS NOT NULL AND corrected_at IS NOT NULL)
    )
);

CREATE TABLE topics (
    id UUID PRIMARY KEY,
    normalized_name VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE journal_topics (
    journal_entry_id UUID NOT NULL REFERENCES journal_entries(id) ON DELETE CASCADE,
    topic_id UUID NOT NULL REFERENCES topics(id) ON DELETE RESTRICT,
    source VARCHAR(20) NOT NULL,
    confidence NUMERIC(5,4),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (journal_entry_id, topic_id),
    CONSTRAINT ck_journal_topic_source CHECK (source IN ('AI', 'USER')),
    CONSTRAINT ck_journal_topic_confidence CHECK (confidence IS NULL OR confidence BETWEEN 0 AND 1)
);

CREATE TABLE journal_corrections (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    journal_entry_id UUID NOT NULL REFERENCES journal_entries(id) ON DELETE CASCADE,
    analysis_id UUID REFERENCES journal_analyses(id) ON DELETE CASCADE,
    field_type VARCHAR(30) NOT NULL,
    field_key VARCHAR(100) NOT NULL,
    operation VARCHAR(20) NOT NULL,
    original_value JSONB,
    corrected_value JSONB,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_correction_field_type CHECK (field_type IN ('EMOTION', 'TOPIC')),
    CONSTRAINT ck_correction_operation CHECK (operation IN ('ADD', 'UPDATE', 'REMOVE'))
);

CREATE INDEX idx_corrections_journal_created
    ON journal_corrections (journal_entry_id, created_at DESC);

DO $$
DECLARE role_name TEXT;
BEGIN
    FOREACH role_name IN ARRAY ARRAY['anon', 'authenticated'] LOOP
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = role_name) THEN
            EXECUTE format('REVOKE ALL PRIVILEGES ON journal_analyses, journal_emotions, topics, journal_topics, journal_corrections FROM %I', role_name);
        END IF;
    END LOOP;
END $$;

ALTER TABLE journal_analyses ENABLE ROW LEVEL SECURITY;
ALTER TABLE journal_emotions ENABLE ROW LEVEL SECURITY;
ALTER TABLE topics ENABLE ROW LEVEL SECURITY;
ALTER TABLE journal_topics ENABLE ROW LEVEL SECURITY;
ALTER TABLE journal_corrections ENABLE ROW LEVEL SECURITY;
