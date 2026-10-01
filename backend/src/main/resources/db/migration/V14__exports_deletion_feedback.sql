CREATE TABLE export_requests (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    format VARCHAR(8) NOT NULL CHECK (format IN ('CSV', 'PDF')),
    status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'READY', 'FAILED', 'EXPIRED')),
    encrypted_file BYTEA,
    file_iv BYTEA,
    file_wrapped_key BYTEA,
    file_key_version VARCHAR(32),
    file_sha256 BYTEA,
    file_size_bytes BIGINT CHECK (file_size_bytes IS NULL OR file_size_bytes >= 0),
    attempt INTEGER NOT NULL DEFAULT 0 CHECK (attempt >= 0),
    available_at TIMESTAMPTZ NOT NULL,
    lease_expires_at TIMESTAMPTZ,
    last_error_code VARCHAR(60),
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    CONSTRAINT export_artifact_complete CHECK (status <> 'READY' OR
        encrypted_file IS NOT NULL AND file_iv IS NOT NULL AND file_wrapped_key IS NOT NULL
        AND file_key_version IS NOT NULL AND file_sha256 IS NOT NULL AND expires_at IS NOT NULL)
);
CREATE UNIQUE INDEX uq_export_active_user ON export_requests(user_id)
    WHERE status IN ('PENDING', 'PROCESSING', 'READY');
CREATE INDEX idx_export_work ON export_requests(status, available_at, lease_expires_at);
CREATE INDEX idx_export_expiry ON export_requests(expires_at) WHERE status = 'READY';
CREATE INDEX idx_export_metadata_purge ON export_requests(expires_at) WHERE status = 'EXPIRED';

CREATE TABLE deletion_requests (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    subject_hash BYTEA NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('GRACE_PERIOD', 'PROCESSING', 'COMPLETED', 'CANCELLED', 'FAILED')),
    requested_at TIMESTAMPTZ NOT NULL,
    scheduled_for TIMESTAMPTZ NOT NULL,
    lease_expires_at TIMESTAMPTZ,
    attempt INTEGER NOT NULL DEFAULT 0 CHECK (attempt >= 0),
    checkpoint VARCHAR(32) NOT NULL DEFAULT 'REQUESTED',
    last_error_code VARCHAR(60),
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_deletion_active_user ON deletion_requests(user_id)
    WHERE status IN ('GRACE_PERIOD', 'PROCESSING', 'FAILED');
CREATE INDEX idx_deletion_work ON deletion_requests(status, scheduled_for, lease_expires_at);
CREATE INDEX idx_deletion_completed_retention ON deletion_requests(completed_at) WHERE status='COMPLETED';
CREATE INDEX idx_deletion_cancelled_retention ON deletion_requests(cancelled_at) WHERE status='CANCELLED';

CREATE TABLE feedback (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category VARCHAR(32) NOT NULL CHECK (category IN ('BUG', 'IDEA', 'OTHER')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')),
    encrypted_message BYTEA NOT NULL,
    message_iv BYTEA NOT NULL,
    message_wrapped_key BYTEA NOT NULL,
    message_key_version VARCHAR(32) NOT NULL,
    assigned_to UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_feedback_status ON feedback(status, created_at DESC);
CREATE INDEX idx_feedback_expiry ON feedback(expires_at);

INSERT INTO permissions (id, code, description, created_at) VALUES
 ('01990000-0000-7000-8000-000000000051', 'feedback:read', 'Read submitted feedback', CURRENT_TIMESTAMP),
 ('01990000-0000-7000-8000-000000000052', 'feedback:manage', 'Assign and resolve feedback', CURRENT_TIMESTAMP);
INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('SYSTEM_ADMIN', 'SUPPORT_AGENT') AND p.code IN ('feedback:read', 'feedback:manage')
ON CONFLICT DO NOTHING;
