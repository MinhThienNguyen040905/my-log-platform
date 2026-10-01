CREATE TABLE users (
    id UUID PRIMARY KEY,
    email_lookup_hash BYTEA NOT NULL,
    encrypted_email BYTEA NOT NULL,
    email_iv BYTEA NOT NULL,
    email_wrapped_key BYTEA NOT NULL,
    email_key_version VARCHAR(32) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    auth_provider VARCHAR(32) NOT NULL CHECK (auth_provider = 'LOCAL'),
    status VARCHAR(24) NOT NULL CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'DELETION_PENDING', 'DELETED')),
    email_verified_at TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    failed_login_count INTEGER NOT NULL DEFAULT 0 CHECK (failed_login_count >= 0),
    locked_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    row_version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_users_status_created ON users (status, created_at DESC);
CREATE UNIQUE INDEX idx_users_active_email ON users (email_lookup_hash) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_deleted ON users (deleted_at) WHERE deleted_at IS NOT NULL;

CREATE TABLE roles (
    id UUID PRIMARY KEY,
    code VARCHAR(60) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    system_role BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE permissions (
    id UUID PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (role_id, permission_id)
);
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id),
    assigned_by UUID REFERENCES users(id) ON DELETE SET NULL,
    assigned_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ,
    PRIMARY KEY (user_id, role_id)
);
CREATE INDEX idx_user_roles_role ON user_roles (role_id);

INSERT INTO roles (id, code, name, description, system_role, created_at)
VALUES ('01990000-0000-7000-8000-000000000001', 'USER', 'User', 'Standard account', TRUE, CURRENT_TIMESTAMP);

CREATE TABLE auth_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_family_id UUID NOT NULL,
    current_token_hash BYTEA NOT NULL UNIQUE,
    device_name VARCHAR(120),
    user_agent_hash BYTEA,
    ip_prefix INET,
    last_used_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    revoke_reason VARCHAR(40),
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_auth_sessions_user ON auth_sessions (user_id, revoked_at, expires_at);
CREATE INDEX idx_auth_sessions_expiry ON auth_sessions (expires_at);
CREATE TABLE auth_refresh_history (
    token_hash BYTEA PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES auth_sessions(id) ON DELETE CASCADE,
    used_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_auth_refresh_history_session ON auth_refresh_history (session_id);

CREATE TABLE auth_action_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash BYTEA NOT NULL UNIQUE,
    purpose VARCHAR(32) NOT NULL CHECK (purpose IN ('VERIFY_EMAIL', 'RESET_PASSWORD')),
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_auth_action_tokens_user ON auth_action_tokens (user_id, purpose, expires_at);

CREATE TABLE auth_rate_limits (
    subject_hash BYTEA PRIMARY KEY,
    attempts INTEGER NOT NULL CHECK (attempts >= 0),
    window_started_at TIMESTAMPTZ NOT NULL,
    blocked_until TIMESTAMPTZ
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    actor_type VARCHAR(24) NOT NULL,
    action VARCHAR(120) NOT NULL,
    target_type VARCHAR(60) NOT NULL,
    target_id UUID,
    reason_code VARCHAR(60),
    safe_metadata JSONB,
    trace_id VARCHAR(64),
    occurred_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_audit_logs_actor_time ON audit_logs (actor_user_id, occurred_at DESC);
