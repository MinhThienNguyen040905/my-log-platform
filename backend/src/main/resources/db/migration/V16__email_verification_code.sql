ALTER TABLE auth_action_tokens
    ADD COLUMN code_hash BYTEA,
    ADD COLUMN code_expires_at TIMESTAMPTZ,
    ADD COLUMN code_failed_attempts INTEGER NOT NULL DEFAULT 0;

ALTER TABLE auth_action_tokens
    ADD CONSTRAINT auth_action_tokens_code_attempts_check
    CHECK (code_failed_attempts >= 0);
