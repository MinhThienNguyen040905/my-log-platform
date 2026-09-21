CREATE TABLE user_consents (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    consent_type VARCHAR(50) NOT NULL,
    policy_version VARCHAR(30) NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    source VARCHAR(30) NOT NULL,
    CONSTRAINT uq_user_consents_policy
        UNIQUE (user_id, consent_type, policy_version),
    CONSTRAINT ck_user_consents_type CHECK (
        consent_type IN ('TERMS', 'PRIVACY', 'AI_PROCESSING')
    ),
    CONSTRAINT ck_user_consents_source CHECK (
        source IN ('WEB', 'ADMIN', 'MIGRATION')
    ),
    CONSTRAINT ck_user_consents_revocation CHECK (
        revoked_at IS NULL OR revoked_at >= granted_at
    )
);

CREATE INDEX idx_user_consents_user_active
    ON user_consents (user_id, consent_type, granted_at DESC)
    WHERE revoked_at IS NULL;

ALTER TABLE refresh_tokens
    ADD COLUMN created_by_ip_hash VARCHAR(128),
    ADD COLUMN user_agent_hash VARCHAR(128);

CREATE INDEX idx_users_status ON users (status);

ALTER TABLE user_consents ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
        EXECUTE 'REVOKE ALL PRIVILEGES ON TABLE user_consents FROM anon';
    END IF;

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        EXECUTE 'REVOKE ALL PRIVILEGES ON TABLE user_consents FROM authenticated';
    END IF;
END
$$;
