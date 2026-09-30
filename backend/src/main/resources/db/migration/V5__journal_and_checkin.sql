CREATE TABLE journal_entries (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    encrypted_payload BYTEA NOT NULL,
    payload_iv BYTEA NOT NULL,
    wrapped_data_key BYTEA NOT NULL,
    encryption_key_version VARCHAR(32) NOT NULL,
    content_format_version SMALLINT NOT NULL CHECK (content_format_version >= 1),
    occurred_at TIMESTAMPTZ NOT NULL,
    local_date DATE NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    mood_code VARCHAR(32),
    mood_score NUMERIC(3,1) CHECK (mood_score BETWEEN 1 AND 10),
    stress_score NUMERIC(3,1) CHECK (stress_score BETWEEN 1 AND 10),
    energy_score NUMERIC(3,1) CHECK (energy_score BETWEEN 1 AND 10),
    sleep_minutes SMALLINT CHECK (sleep_minutes BETWEEN 0 AND 1440),
    favorite BOOLEAN NOT NULL DEFAULT FALSE,
    entry_status VARCHAR(24) NOT NULL CHECK (entry_status IN ('DRAFT','SAVED','DELETED')),
    risk_level VARCHAR(16) NOT NULL CHECK (risk_level IN ('UNKNOWN','NORMAL','LOW','MODERATE','HIGH','CRITICAL')),
    analysis_status VARCHAR(32) NOT NULL CHECK (analysis_status IN
        ('NOT_REQUESTED','PENDING','ANALYZING','ANALYZED','ANALYSIS_FAILED','ANALYSIS_OUTDATED','BLOCKED_BY_SAFETY')),
    content_version INTEGER NOT NULL DEFAULT 1 CHECK (content_version >= 1),
    latest_analysis_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    row_version BIGINT NOT NULL DEFAULT 0 CHECK (row_version >= 0)
);
CREATE INDEX idx_journal_entries_user_occurred ON journal_entries(user_id, occurred_at DESC, id DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_journal_entries_user_date ON journal_entries(user_id, local_date DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_journal_entries_favorite ON journal_entries(user_id, occurred_at DESC) WHERE favorite AND deleted_at IS NULL;
CREATE INDEX idx_journal_entries_analysis ON journal_entries(analysis_status, updated_at)
    WHERE analysis_status IN ('PENDING','ANALYSIS_FAILED','ANALYSIS_OUTDATED');

CREATE TABLE journal_tags (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name_lookup_hash BYTEA NOT NULL,
    encrypted_name BYTEA NOT NULL,
    name_iv BYTEA NOT NULL,
    name_wrapped_key BYTEA NOT NULL,
    name_key_version VARCHAR(32) NOT NULL,
    color VARCHAR(9) CHECK (color IS NULL OR color ~ '^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$'),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE(user_id, name_lookup_hash)
);
CREATE INDEX idx_journal_tags_user ON journal_tags(user_id);

CREATE TABLE journal_entry_tags (
    journal_entry_id UUID NOT NULL REFERENCES journal_entries(id) ON DELETE CASCADE,
    tag_id UUID NOT NULL REFERENCES journal_tags(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(journal_entry_id, tag_id)
);
CREATE INDEX idx_journal_entry_tags_tag ON journal_entry_tags(tag_id);

CREATE TABLE journal_assets (
    id UUID PRIMARY KEY,
    journal_entry_id UUID NOT NULL REFERENCES journal_entries(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider_asset_id VARCHAR(255) NOT NULL UNIQUE,
    public_id VARCHAR(512) NOT NULL UNIQUE,
    provider_version BIGINT NOT NULL,
    format VARCHAR(32) NOT NULL,
    delivery_type VARCHAR(32) NOT NULL CHECK (delivery_type IN ('AUTHENTICATED','PRIVATE')),
    asset_type VARCHAR(24) NOT NULL CHECK (asset_type IN ('IMAGE','ATTACHMENT')),
    mime_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes >= 0),
    sha256 BYTEA NOT NULL,
    width INTEGER CHECK (width IS NULL OR width > 0),
    height INTEGER CHECK (height IS NULL OR height > 0),
    status VARCHAR(24) NOT NULL CHECK (status IN ('PENDING_SCAN','READY','REJECTED','DELETED')),
    encrypted_caption BYTEA,
    caption_iv BYTEA,
    caption_wrapped_key BYTEA,
    caption_key_version VARCHAR(32),
    created_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_journal_assets_entry ON journal_assets(journal_entry_id, user_id);

CREATE TABLE daily_checkins (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    local_date DATE NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    mood_code VARCHAR(32),
    mood_score NUMERIC(3,1) CHECK (mood_score BETWEEN 1 AND 10),
    stress_score NUMERIC(3,1) CHECK (stress_score BETWEEN 1 AND 10),
    energy_score NUMERIC(3,1) CHECK (energy_score BETWEEN 1 AND 10),
    sleep_minutes SMALLINT CHECK (sleep_minutes BETWEEN 0 AND 1440),
    encrypted_note BYTEA,
    note_iv BYTEA,
    note_wrapped_key BYTEA,
    note_key_version VARCHAR(32),
    source VARCHAR(24) NOT NULL CHECK (source IN ('USER','JOURNAL_FALLBACK','IMPORT')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0 CHECK (row_version >= 0),
    UNIQUE(user_id, local_date),
    CHECK ((encrypted_note IS NULL AND note_iv IS NULL AND note_wrapped_key IS NULL AND note_key_version IS NULL)
        OR (encrypted_note IS NOT NULL AND note_iv IS NOT NULL AND note_wrapped_key IS NOT NULL AND note_key_version IS NOT NULL))
);
CREATE INDEX idx_daily_checkins_user_date ON daily_checkins(user_id, local_date DESC);

CREATE TABLE checkin_activities (
    id UUID PRIMARY KEY,
    checkin_id UUID NOT NULL REFERENCES daily_checkins(id) ON DELETE CASCADE,
    activity_code VARCHAR(40) NOT NULL,
    duration_minutes SMALLINT CHECK (duration_minutes BETWEEN 0 AND 1440),
    intensity VARCHAR(16) CHECK (intensity IN ('LOW','MODERATE','HIGH')),
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE(checkin_id, activity_code)
);
