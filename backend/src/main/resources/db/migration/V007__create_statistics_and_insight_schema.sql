CREATE TABLE daily_user_statistics (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    entry_date DATE NOT NULL,
    journal_count INTEGER NOT NULL,
    mood_average NUMERIC(5,2),
    stress_average NUMERIC(5,2),
    energy_average NUMERIC(5,2),
    calculation_version VARCHAR(50) NOT NULL,
    calculated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, entry_date),
    CONSTRAINT ck_daily_user_statistics_count CHECK (journal_count >= 0)
);

CREATE TABLE daily_emotion_statistics (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    entry_date DATE NOT NULL,
    emotion_type VARCHAR(30) NOT NULL,
    sample_size INTEGER NOT NULL,
    average_score NUMERIC(5,4) NOT NULL,
    calculation_version VARCHAR(50) NOT NULL,
    calculated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, entry_date, emotion_type),
    CONSTRAINT ck_daily_emotion_sample CHECK (sample_size >= 0),
    CONSTRAINT ck_daily_emotion_average CHECK (average_score BETWEEN 0 AND 1)
);

CREATE TABLE insights (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    fingerprint VARCHAR(128) NOT NULL,
    title VARCHAR(250) NOT NULL,
    description TEXT NOT NULL,
    confidence VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    explanation_provider VARCHAR(50),
    explanation_model VARCHAR(100),
    explanation_prompt_version VARCHAR(30),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_insight_fingerprint_period UNIQUE (user_id, fingerprint, period_start, period_end),
    CONSTRAINT ck_insight_confidence CHECK (confidence IN ('WEAK', 'MODERATE', 'STRONG')),
    CONSTRAINT ck_insight_status CHECK (status IN ('ACTIVE', 'FADING', 'EXPIRED', 'DISMISSED')),
    CONSTRAINT ck_insight_period CHECK (period_start <= period_end)
);

CREATE TABLE insight_evidence (
    id UUID PRIMARY KEY,
    insight_id UUID NOT NULL REFERENCES insights(id) ON DELETE CASCADE,
    evidence_type VARCHAR(50) NOT NULL,
    sample_size INTEGER NOT NULL,
    matching_count INTEGER,
    metric VARCHAR(100) NOT NULL,
    numeric_value NUMERIC(18,6),
    unit VARCHAR(30),
    evidence_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    calculation_version VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_evidence_sample_size CHECK (sample_size >= 0),
    CONSTRAINT ck_evidence_matching_count CHECK (
        matching_count IS NULL OR matching_count BETWEEN 0 AND sample_size
    ),
    CONSTRAINT ck_evidence_json_object CHECK (jsonb_typeof(evidence_json) = 'object')
);

CREATE TABLE suggested_actions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    insight_id UUID NOT NULL REFERENCES insights(id) ON DELETE CASCADE,
    description TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    provider VARCHAR(50),
    model VARCHAR(100),
    prompt_version VARCHAR(30),
    created_at TIMESTAMPTZ NOT NULL,
    responded_at TIMESTAMPTZ,
    CONSTRAINT ck_action_status CHECK (status IN ('PENDING', 'ACCEPTED', 'IGNORED')),
    CONSTRAINT ck_action_description_not_blank CHECK (length(btrim(description)) > 0)
);

CREATE TABLE feedback (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_type VARCHAR(30) NOT NULL,
    target_id UUID NOT NULL,
    value VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_feedback_user_target UNIQUE (user_id, target_type, target_id),
    CONSTRAINT ck_feedback_target_type CHECK (target_type IN ('REFLECTION', 'ACTION', 'INSIGHT')),
    CONSTRAINT ck_feedback_value CHECK (value IN ('HELPFUL', 'NOT_HELPFUL'))
);

ALTER TABLE daily_user_statistics ENABLE ROW LEVEL SECURITY;
ALTER TABLE daily_emotion_statistics ENABLE ROW LEVEL SECURITY;
ALTER TABLE insights ENABLE ROW LEVEL SECURITY;
ALTER TABLE insight_evidence ENABLE ROW LEVEL SECURITY;
ALTER TABLE suggested_actions ENABLE ROW LEVEL SECURITY;
ALTER TABLE feedback ENABLE ROW LEVEL SECURITY;

DO $$
DECLARE role_name TEXT;
BEGIN
    FOREACH role_name IN ARRAY ARRAY['anon', 'authenticated'] LOOP
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = role_name) THEN
            EXECUTE format(
                'REVOKE ALL PRIVILEGES ON daily_user_statistics, daily_emotion_statistics, insights, insight_evidence, suggested_actions, feedback FROM %I',
                role_name);
        END IF;
    END LOOP;
END $$;
