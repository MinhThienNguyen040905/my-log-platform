CREATE TABLE user_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    encrypted_profile BYTEA NOT NULL,
    profile_iv BYTEA NOT NULL,
    profile_wrapped_key BYTEA NOT NULL,
    profile_key_version VARCHAR(32) NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    locale VARCHAR(10) NOT NULL,
    onboarding_completed_at TIMESTAMPTZ,
    preferred_journal_time TIME,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE user_consents (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    consent_type VARCHAR(40) NOT NULL CHECK (consent_type IN ('TERMS', 'PRIVACY', 'AI_PROCESSING', 'ANALYTICS', 'MODEL_TRAINING')),
    document_version VARCHAR(40) NOT NULL,
    granted BOOLEAN NOT NULL,
    decided_at TIMESTAMPTZ NOT NULL,
    source VARCHAR(24) NOT NULL CHECK (source IN ('ONBOARDING', 'SETTINGS', 'ADMIN_IMPORT'))
);
CREATE INDEX idx_user_consents_latest ON user_consents (user_id, consent_type, decided_at DESC);
