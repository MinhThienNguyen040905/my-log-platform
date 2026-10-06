ALTER TABLE outbox_events ADD COLUMN lease_expires_at TIMESTAMPTZ;
ALTER TABLE outbox_events ADD COLUMN last_error_code VARCHAR(80);
CREATE INDEX idx_outbox_recovery ON outbox_events(lease_expires_at)
    WHERE status = 'PROCESSING';

CREATE TABLE ai_jobs (
    id UUID PRIMARY KEY,
    job_type VARCHAR(40) NOT NULL,
    aggregate_type VARCHAR(40) NOT NULL,
    aggregate_id UUID NOT NULL,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(24) NOT NULL CHECK (status IN ('PENDING','RETRY_WAIT','PROCESSING','SUCCEEDED','FAILED','DEAD')),
    priority SMALLINT NOT NULL DEFAULT 100,
    attempt INTEGER NOT NULL DEFAULT 0 CHECK (attempt >= 0),
    max_attempts INTEGER NOT NULL CHECK (max_attempts > 0),
    available_at TIMESTAMPTZ NOT NULL,
    locked_at TIMESTAMPTZ,
    locked_by VARCHAR(120),
    lease_expires_at TIMESTAMPTZ,
    idempotency_key VARCHAR(160) NOT NULL UNIQUE,
    payload JSONB NOT NULL,
    payload_version SMALLINT NOT NULL CHECK (payload_version > 0),
    last_error_code VARCHAR(80),
    last_error_summary VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ
);
CREATE INDEX idx_ai_jobs_claim ON ai_jobs(priority, available_at, created_at)
    WHERE status IN ('PENDING','RETRY_WAIT');
CREATE INDEX idx_ai_jobs_recovery ON ai_jobs(lease_expires_at)
    WHERE status = 'PROCESSING';
CREATE INDEX idx_ai_jobs_aggregate ON ai_jobs(aggregate_type, aggregate_id, created_at DESC);

CREATE TABLE ai_analyses (
    id UUID PRIMARY KEY,
    journal_entry_id UUID NOT NULL REFERENCES journal_entries(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content_version INTEGER NOT NULL CHECK (content_version > 0),
    analysis_version INTEGER NOT NULL CHECK (analysis_version > 0),
    status VARCHAR(24) NOT NULL CHECK (status IN ('RUNNING','SUCCEEDED','FAILED','STALE','REJECTED_BY_SAFETY')),
    sentiment_label VARCHAR(24) CHECK (sentiment_label IN ('POSITIVE','NEUTRAL','NEGATIVE','MIXED')),
    sentiment_score NUMERIC(6,5) CHECK (sentiment_score BETWEEN 0 AND 1),
    encrypted_output BYTEA,
    output_iv BYTEA,
    wrapped_data_key BYTEA,
    encryption_key_version VARCHAR(32),
    output_schema_version SMALLINT NOT NULL CHECK (output_schema_version > 0),
    provider VARCHAR(40) NOT NULL,
    model VARCHAR(120) NOT NULL,
    model_version VARCHAR(120),
    prompt_template_version VARCHAR(40) NOT NULL,
    safety_policy_version VARCHAR(40) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE(journal_entry_id, content_version, analysis_version)
);
CREATE UNIQUE INDEX uq_ai_analyses_success ON ai_analyses(journal_entry_id, content_version)
    WHERE status = 'SUCCEEDED';
CREATE INDEX idx_ai_analyses_user_entry ON ai_analyses(user_id, journal_entry_id, created_at DESC);
ALTER TABLE journal_entries ADD CONSTRAINT fk_journal_latest_analysis
    FOREIGN KEY (latest_analysis_id) REFERENCES ai_analyses(id) ON DELETE SET NULL;

CREATE TABLE analysis_emotions (
    analysis_id UUID NOT NULL REFERENCES ai_analyses(id) ON DELETE CASCADE,
    emotion_code VARCHAR(32) NOT NULL,
    score NUMERIC(6,5) NOT NULL CHECK (score BETWEEN 0 AND 1),
    rank SMALLINT NOT NULL CHECK (rank BETWEEN 1 AND 5),
    PRIMARY KEY (analysis_id, emotion_code),
    UNIQUE (analysis_id, rank)
);
CREATE TABLE analysis_topics (
    id UUID PRIMARY KEY,
    analysis_id UUID NOT NULL REFERENCES ai_analyses(id) ON DELETE CASCADE,
    topic_code VARCHAR(48) NOT NULL,
    score NUMERIC(6,5) NOT NULL CHECK (score BETWEEN 0 AND 1),
    rank SMALLINT NOT NULL CHECK (rank BETWEEN 1 AND 5),
    UNIQUE (analysis_id, topic_code),
    UNIQUE (analysis_id, rank)
);

CREATE TABLE ai_usage_records (
    id UUID PRIMARY KEY,
    job_id UUID REFERENCES ai_jobs(id) ON DELETE SET NULL,
    analysis_id UUID REFERENCES ai_analyses(id) ON DELETE SET NULL,
    provider VARCHAR(40) NOT NULL,
    model VARCHAR(120) NOT NULL,
    operation VARCHAR(32) NOT NULL,
    input_tokens INTEGER NOT NULL CHECK (input_tokens >= 0),
    output_tokens INTEGER NOT NULL CHECK (output_tokens >= 0),
    estimated_cost_usd NUMERIC(12,6) NOT NULL CHECK (estimated_cost_usd >= 0),
    latency_ms INTEGER NOT NULL CHECK (latency_ms >= 0),
    request_status VARCHAR(24) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_ai_usage_time ON ai_usage_records(created_at DESC, provider, operation);
