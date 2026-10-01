CREATE TABLE insights (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    insight_type VARCHAR(40) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL CHECK (status IN ('ACTIVE','SUPERSEDED','HIDDEN')),
    direction VARCHAR(16) CHECK (direction IN ('UP','DOWN','STABLE')),
    strength NUMERIC(6,5) CHECK (strength BETWEEN 0 AND 1),
    sample_size INTEGER NOT NULL CHECK (sample_size >= 0),
    algorithm_version VARCHAR(40) NOT NULL,
    metrics_snapshot JSONB NOT NULL,
    encrypted_narrative BYTEA,
    narrative_iv BYTEA,
    narrative_wrapped_key BYTEA,
    narrative_key_version VARCHAR(32),
    generated_by VARCHAR(24) NOT NULL CHECK (generated_by IN ('RULE','STATISTICAL','AI_ASSISTED')),
    created_at TIMESTAMPTZ NOT NULL,
    superseded_at TIMESTAMPTZ,
    UNIQUE(user_id, insight_type, period_start, period_end, algorithm_version),
    CHECK (period_start <= period_end),
    CHECK ((encrypted_narrative IS NULL AND narrative_iv IS NULL AND narrative_wrapped_key IS NULL AND narrative_key_version IS NULL)
        OR (encrypted_narrative IS NOT NULL AND narrative_iv IS NOT NULL AND narrative_wrapped_key IS NOT NULL AND narrative_key_version IS NOT NULL))
);
CREATE INDEX idx_insights_user_period ON insights(user_id, period_end DESC, id DESC)
    WHERE status = 'ACTIVE';

CREATE TABLE insight_evidence (
    id UUID PRIMARY KEY,
    insight_id UUID NOT NULL REFERENCES insights(id) ON DELETE CASCADE,
    source_type VARCHAR(32) NOT NULL CHECK (source_type IN ('CHECKIN','JOURNAL','STRUCTURED_METRIC')),
    source_id UUID,
    evidence_date DATE NOT NULL,
    metric_name VARCHAR(48) NOT NULL,
    metric_value NUMERIC(12,4),
    weight NUMERIC(6,5) CHECK (weight BETWEEN 0 AND 1),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_insight_evidence_insight ON insight_evidence(insight_id, evidence_date);

CREATE TABLE reports (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    report_type VARCHAR(16) NOT NULL CHECK (report_type IN ('WEEKLY','MONTHLY')),
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    version INTEGER NOT NULL CHECK (version > 0),
    status VARCHAR(24) NOT NULL CHECK (status IN ('PENDING','PROCESSING','READY','FAILED','DEAD')),
    sample_size INTEGER NOT NULL DEFAULT 0 CHECK (sample_size >= 0),
    metrics_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    encrypted_narrative BYTEA,
    narrative_iv BYTEA,
    narrative_wrapped_key BYTEA,
    narrative_key_version VARCHAR(32),
    generated_by VARCHAR(24) NOT NULL DEFAULT 'RULE' CHECK (generated_by IN ('RULE','AI_ASSISTED')),
    model VARCHAR(120),
    prompt_template_version VARCHAR(40),
    safety_policy_version VARCHAR(40),
    attempt INTEGER NOT NULL DEFAULT 0 CHECK (attempt >= 0),
    available_at TIMESTAMPTZ NOT NULL,
    locked_at TIMESTAMPTZ,
    locked_by VARCHAR(120),
    lease_expires_at TIMESTAMPTZ,
    last_error_code VARCHAR(80),
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    UNIQUE(user_id, report_type, period_start, version),
    CHECK (period_start <= period_end),
    CHECK ((encrypted_narrative IS NULL AND narrative_iv IS NULL AND narrative_wrapped_key IS NULL AND narrative_key_version IS NULL)
        OR (encrypted_narrative IS NOT NULL AND narrative_iv IS NOT NULL AND narrative_wrapped_key IS NOT NULL AND narrative_key_version IS NOT NULL))
);
CREATE INDEX idx_reports_user_period ON reports(user_id, period_end DESC, id DESC);
CREATE INDEX idx_reports_claim ON reports(available_at, created_at) WHERE status IN ('PENDING','FAILED');
CREATE INDEX idx_reports_recovery ON reports(lease_expires_at) WHERE status = 'PROCESSING';

CREATE TABLE report_evidence (
    id UUID PRIMARY KEY,
    report_id UUID NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
    insight_id UUID REFERENCES insights(id) ON DELETE SET NULL,
    source_type VARCHAR(32) NOT NULL CHECK (source_type IN ('CHECKIN','JOURNAL','STRUCTURED_METRIC')),
    source_id UUID,
    evidence_date DATE NOT NULL,
    metric_name VARCHAR(48) NOT NULL,
    metric_value NUMERIC(12,4),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_report_evidence_report ON report_evidence(report_id, evidence_date);
