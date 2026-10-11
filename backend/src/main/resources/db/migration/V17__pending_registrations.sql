-- A user is created only after the email challenge succeeds. Legacy unverified
-- accounts cannot log in and are removed so their addresses can register again.
DELETE FROM users WHERE status = 'PENDING';
ALTER TABLE users DROP CONSTRAINT users_status_check;
ALTER TABLE users ADD CONSTRAINT users_status_check
    CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETION_PENDING', 'DELETED'));

CREATE TABLE pending_registrations (
    id UUID PRIMARY KEY,
    email_lookup_hash BYTEA NOT NULL UNIQUE,
    encrypted_email BYTEA NOT NULL,
    email_iv BYTEA NOT NULL,
    email_wrapped_key BYTEA NOT NULL,
    email_key_version VARCHAR(32) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    locale VARCHAR(10) NOT NULL,
    terms_version VARCHAR(40) NOT NULL,
    privacy_version VARCHAR(40) NOT NULL,
    token_hash BYTEA NOT NULL UNIQUE,
    code_hash BYTEA NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    code_expires_at TIMESTAMPTZ NOT NULL,
    code_failed_attempts INTEGER NOT NULL DEFAULT 0 CHECK (code_failed_attempts >= 0),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_pending_registrations_expiry ON pending_registrations (expires_at);

REVOKE ALL PRIVILEGES ON TABLE pending_registrations FROM PUBLIC;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
        REVOKE ALL PRIVILEGES ON TABLE pending_registrations FROM anon;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        REVOKE ALL PRIVILEGES ON TABLE pending_registrations FROM authenticated;
    END IF;
END $$;
ALTER TABLE pending_registrations ENABLE ROW LEVEL SECURITY;
