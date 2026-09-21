CREATE TABLE reflection_questions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    journal_entry_id UUID NOT NULL REFERENCES journal_entries(id) ON DELETE CASCADE,
    journal_version BIGINT NOT NULL,
    generation_batch_id UUID NOT NULL,
    position SMALLINT NOT NULL,
    question TEXT NOT NULL,
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_reflection_batch_position UNIQUE (generation_batch_id, position),
    CONSTRAINT ck_reflection_position CHECK (position BETWEEN 1 AND 10),
    CONSTRAINT ck_reflection_question_not_blank CHECK (length(btrim(question)) > 0)
);

CREATE INDEX idx_reflection_current
    ON reflection_questions (journal_entry_id, journal_version, created_at DESC);

CREATE TABLE reflection_responses (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reflection_question_id UUID NOT NULL REFERENCES reflection_questions(id) ON DELETE CASCADE,
    response_text TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_reflection_response_user_question UNIQUE (user_id, reflection_question_id),
    CONSTRAINT ck_reflection_response_not_blank CHECK (length(btrim(response_text)) > 0)
);

CREATE TABLE safety_events (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    journal_entry_id UUID REFERENCES journal_entries(id) ON DELETE SET NULL,
    journal_version BIGINT,
    risk_level VARCHAR(20) NOT NULL,
    detection_source VARCHAR(30) NOT NULL,
    action_taken VARCHAR(100) NOT NULL,
    provider VARCHAR(50),
    model VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_safety_risk CHECK (risk_level IN ('NORMAL', 'LOW', 'MODERATE', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_safety_source CHECK (detection_source IN ('RULE', 'AI', 'COMBINED'))
);

CREATE INDEX idx_safety_events_user_created ON safety_events (user_id, created_at DESC);

CREATE TABLE ai_usage_records (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    journal_entry_id UUID REFERENCES journal_entries(id) ON DELETE SET NULL,
    job_id UUID REFERENCES analysis_jobs(id) ON DELETE SET NULL,
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(100) NOT NULL,
    operation VARCHAR(30) NOT NULL,
    input_token_count INTEGER,
    output_token_count INTEGER,
    latency_ms BIGINT NOT NULL,
    success BOOLEAN NOT NULL,
    error_code VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_ai_usage_operation CHECK (operation IN ('ANALYSIS', 'REFLECTION')),
    CONSTRAINT ck_ai_usage_tokens CHECK (
        (input_token_count IS NULL OR input_token_count >= 0)
        AND (output_token_count IS NULL OR output_token_count >= 0)
    ),
    CONSTRAINT ck_ai_usage_latency CHECK (latency_ms >= 0)
);

CREATE INDEX idx_ai_usage_user_created ON ai_usage_records (user_id, created_at DESC);

DO $$
DECLARE role_name TEXT;
BEGIN
    FOREACH role_name IN ARRAY ARRAY['anon', 'authenticated'] LOOP
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = role_name) THEN
            EXECUTE format('REVOKE ALL PRIVILEGES ON reflection_questions, reflection_responses, safety_events, ai_usage_records FROM %I', role_name);
        END IF;
    END LOOP;
END $$;

ALTER TABLE reflection_questions ENABLE ROW LEVEL SECURITY;
ALTER TABLE reflection_responses ENABLE ROW LEVEL SECURITY;
ALTER TABLE safety_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_usage_records ENABLE ROW LEVEL SECURITY;
