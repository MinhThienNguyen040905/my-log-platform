-- Files to remove from asset storage (Cloudinary or local) after their journal_assets rows are deleted by
-- "start over" or account deletion. Rows are written in the same transaction as the data deletion, so a storage
-- outage never loses track of a file. No FK to users: rows must outlive a deleted account.
CREATE TABLE asset_deletion_queue (
    id UUID PRIMARY KEY,
    public_id VARCHAR(512) NOT NULL,
    asset_type VARCHAR(24) NOT NULL CHECK (asset_type IN ('IMAGE','ATTACHMENT')),
    delivery_type VARCHAR(32) NOT NULL CHECK (delivery_type IN ('AUTHENTICATED','PRIVATE')),
    attempt INTEGER NOT NULL DEFAULT 0 CHECK (attempt >= 0),
    available_at TIMESTAMPTZ NOT NULL,
    lease_expires_at TIMESTAMPTZ,
    last_error_code VARCHAR(60),
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_asset_deletion_queue_work ON asset_deletion_queue(available_at, lease_expires_at);

-- ADR-0008: the Spring backend is the only data API.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
        REVOKE ALL PRIVILEGES ON TABLE asset_deletion_queue FROM anon;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        REVOKE ALL PRIVILEGES ON TABLE asset_deletion_queue FROM authenticated;
    END IF;
END $$;
ALTER TABLE asset_deletion_queue ENABLE ROW LEVEL SECURITY;
