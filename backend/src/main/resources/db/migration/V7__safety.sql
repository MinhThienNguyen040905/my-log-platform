CREATE TABLE safety_policy_versions (
    id UUID PRIMARY KEY,
    version VARCHAR(40) NOT NULL UNIQUE,
    status VARCHAR(24) NOT NULL CHECK (status IN ('DRAFT','APPROVED','RETIRED')),
    config JSONB NOT NULL,
    approved_by UUID REFERENCES users(id) ON DELETE SET NULL,
    approved_at TIMESTAMPTZ,
    effective_from TIMESTAMPTZ,
    effective_to TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE safety_resources (
    id UUID PRIMARY KEY,
    locale VARCHAR(10) NOT NULL,
    country_code VARCHAR(2) NOT NULL CHECK (length(country_code) = 2),
    resource_type VARCHAR(32) NOT NULL,
    name VARCHAR(200) NOT NULL,
    contact_value VARCHAR(300),
    description TEXT,
    source_url TEXT,
    verified_at TIMESTAMPTZ,
    status VARCHAR(24) NOT NULL CHECK (status IN ('DRAFT','APPROVED','RETIRED')),
    version INTEGER NOT NULL CHECK (version >= 1),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_safety_resources_locale ON safety_resources(locale, country_code, status);

CREATE TABLE safety_events (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    subject_ref BYTEA NOT NULL,
    journal_entry_id UUID REFERENCES journal_entries(id) ON DELETE SET NULL,
    risk_level VARCHAR(16) NOT NULL CHECK (risk_level IN ('NORMAL','LOW','MODERATE','HIGH','CRITICAL','UNKNOWN')),
    decision VARCHAR(32) NOT NULL CHECK (decision IN ('ALLOW','CONSTRAIN','SAFETY_FLOW','FAIL_SAFE')),
    rule_version VARCHAR(40),
    classifier VARCHAR(80),
    classifier_version VARCHAR(80),
    policy_version VARCHAR(40) NOT NULL,
    confidence NUMERIC(6,5) CHECK (confidence BETWEEN 0 AND 1),
    resource_set_version VARCHAR(40),
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_safety_events_risk_time ON safety_events(created_at DESC, risk_level);
CREATE INDEX idx_safety_events_entry ON safety_events(journal_entry_id);
