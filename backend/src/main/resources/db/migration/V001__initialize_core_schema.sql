CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    email_normalized VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    plan VARCHAR(20) NOT NULL DEFAULT 'FREE',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    email_verified_at TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_users_email_normalized UNIQUE (email_normalized),
    CONSTRAINT ck_users_plan CHECK (plan IN ('FREE', 'PLUS', 'ADMIN')),
    CONSTRAINT ck_users_status CHECK (
        status IN ('ACTIVE', 'LOCKED', 'DELETION_PENDING', 'DELETED')
    )
);

CREATE TABLE user_preferences (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    timezone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    language VARCHAR(10) NOT NULL DEFAULT 'vi',
    is_onboarded BOOLEAN NOT NULL DEFAULT FALSE,
    preferred_journal_time TIME,
    journaling_goals JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_user_preferences_language CHECK (language IN ('vi', 'en')),
    CONSTRAINT ck_user_preferences_goals_array CHECK (
        jsonb_typeof(journaling_goals) = 'array'
    )
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    family_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by_token_id UUID REFERENCES refresh_tokens(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_refresh_tokens_user_active
    ON refresh_tokens (user_id, expires_at)
    WHERE revoked_at IS NULL;

CREATE INDEX idx_refresh_tokens_family
    ON refresh_tokens (family_id);

CREATE TABLE journal_entries (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(200),
    content_text TEXT NOT NULL,
    content_json JSONB,
    content_format VARCHAR(20) NOT NULL DEFAULT 'PLAIN_TEXT',
    mood_score SMALLINT NOT NULL,
    stress_score SMALLINT,
    energy_score SMALLINT,
    status VARCHAR(30) NOT NULL DEFAULT 'SAVED',
    journal_version BIGINT NOT NULL DEFAULT 1,
    occurred_at TIMESTAMPTZ NOT NULL,
    entry_date DATE NOT NULL,
    timezone_at_entry VARCHAR(64) NOT NULL,
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_journal_content_not_blank CHECK (length(btrim(content_text)) > 0),
    CONSTRAINT ck_journal_content_format CHECK (
        content_format IN ('PLAIN_TEXT', 'TIPTAP_JSON')
    ),
    CONSTRAINT ck_journal_mood CHECK (mood_score BETWEEN 1 AND 10),
    CONSTRAINT ck_journal_stress CHECK (
        stress_score IS NULL OR stress_score BETWEEN 1 AND 10
    ),
    CONSTRAINT ck_journal_energy CHECK (
        energy_score IS NULL OR energy_score BETWEEN 1 AND 10
    ),
    CONSTRAINT ck_journal_status CHECK (
        status IN (
            'SAVED', 'ANALYZING', 'ANALYZED',
            'ANALYSIS_FAILED', 'ANALYSIS_OUTDATED'
        )
    ),
    CONSTRAINT ck_journal_version_positive CHECK (journal_version >= 1)
);

CREATE INDEX idx_journal_entries_user_created
    ON journal_entries (user_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_journal_entries_user_entry_date
    ON journal_entries (user_id, entry_date DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_journal_entries_user_status
    ON journal_entries (user_id, status)
    WHERE deleted_at IS NULL;

CREATE TABLE analysis_jobs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    journal_entry_id UUID NOT NULL REFERENCES journal_entries(id) ON DELETE CASCADE,
    journal_version BIGINT NOT NULL,
    job_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 4,
    next_attempt_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    last_error_code VARCHAR(100),
    last_error_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_analysis_job_version_type
        UNIQUE (journal_entry_id, journal_version, job_type),
    CONSTRAINT ck_analysis_job_type CHECK (
        job_type IN ('ANALYSIS', 'REFLECTION', 'STATISTICS', 'INSIGHT')
    ),
    CONSTRAINT ck_analysis_job_status CHECK (
        status IN (
            'PENDING', 'PROCESSING', 'RETRY_WAIT', 'COMPLETED',
            'FAILED', 'CANCELLED', 'OBSOLETE'
        )
    ),
    CONSTRAINT ck_analysis_job_attempts CHECK (
        attempt_count >= 0 AND max_attempts > 0
    )
);

CREATE INDEX idx_analysis_jobs_claim
    ON analysis_jobs (status, next_attempt_at, created_at)
    WHERE status IN ('PENDING', 'RETRY_WAIT');

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_version INTEGER NOT NULL DEFAULT 1,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    last_error_code VARCHAR(100),
    CONSTRAINT ck_outbox_status CHECK (
        status IN ('PENDING', 'PUBLISHING', 'PUBLISHED', 'FAILED')
    ),
    CONSTRAINT ck_outbox_attempt_count CHECK (attempt_count >= 0),
    CONSTRAINT ck_outbox_payload_object CHECK (jsonb_typeof(payload) = 'object')
);

CREATE INDEX idx_outbox_events_claim
    ON outbox_events (status, next_attempt_at, occurred_at)
    WHERE status IN ('PENDING', 'FAILED');

CREATE TABLE processed_messages (
    consumer_name VARCHAR(100) NOT NULL,
    message_id UUID NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (consumer_name, message_id)
);

CREATE TABLE idempotency_records (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    idempotency_key VARCHAR(100) NOT NULL,
    request_method VARCHAR(10) NOT NULL,
    request_path VARCHAR(255) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    response_status INTEGER,
    response_body JSONB,
    resource_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_idempotency_request
        UNIQUE (user_id, request_method, request_path, idempotency_key),
    CONSTRAINT ck_idempotency_status CHECK (
        status IN ('PROCESSING', 'COMPLETED', 'FAILED')
    )
);

CREATE INDEX idx_idempotency_expiry ON idempotency_records (expires_at);
