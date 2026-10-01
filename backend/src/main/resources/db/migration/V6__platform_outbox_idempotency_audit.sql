CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(40) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    event_version SMALLINT NOT NULL CHECK (event_version >= 1),
    payload JSONB NOT NULL,
    status VARCHAR(24) NOT NULL CHECK (status IN ('PENDING','PROCESSING','PUBLISHED','FAILED','DEAD')),
    attempt INTEGER NOT NULL DEFAULT 0 CHECK (attempt >= 0),
    available_at TIMESTAMPTZ NOT NULL,
    locked_at TIMESTAMPTZ,
    locked_by VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);
CREATE INDEX idx_outbox_claim ON outbox_events(available_at, created_at)
    WHERE status IN ('PENDING','FAILED');
CREATE INDEX idx_outbox_aggregate ON outbox_events(aggregate_type, aggregate_id, created_at);

CREATE TABLE idempotency_keys (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    operation VARCHAR(80) NOT NULL,
    idempotency_key VARCHAR(160) NOT NULL,
    request_hash BYTEA NOT NULL,
    response_status INTEGER,
    response_reference UUID,
    state VARCHAR(24) NOT NULL CHECK (state IN ('PROCESSING','COMPLETED','FAILED')),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    UNIQUE(user_id, operation, idempotency_key)
);
CREATE INDEX idx_idempotency_expiry ON idempotency_keys(expires_at);
CREATE INDEX idx_audit_logs_target_time ON audit_logs(target_type, target_id, occurred_at DESC);
