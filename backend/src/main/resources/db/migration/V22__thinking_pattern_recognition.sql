-- Thinking-pattern recognition (taxonomy tp-v1). See docs/THINKING_PATTERN_RECOGNITION_PLAN.md.
-- Pattern codes are validated by the application against the versioned taxonomy; the database only checks
-- their shape so a new taxonomy version does not need a migration.

-- Separate opt-in, off by default: reading journal text for thinking patterns is a purpose of its own.
ALTER TABLE user_consents DROP CONSTRAINT user_consents_consent_type_check;
ALTER TABLE user_consents ADD CONSTRAINT user_consents_consent_type_check
    CHECK (consent_type IN ('TERMS', 'PRIVACY', 'AI_PROCESSING', 'ANALYTICS', 'MODEL_TRAINING', 'THINKING_PATTERNS'));

-- One row per (entry, content version, taxonomy) that the extractor has read, including reads that found
-- nothing, so unchanged entries are not sent to the extractor again. Owned by the analysis module.
CREATE TABLE entry_pattern_extractions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    journal_entry_id UUID NOT NULL REFERENCES journal_entries(id) ON DELETE CASCADE,
    content_version INTEGER NOT NULL CHECK (content_version > 0),
    taxonomy_version VARCHAR(16) NOT NULL CHECK (taxonomy_version ~ '^[a-z0-9.-]{1,16}$'),
    signal_count INTEGER NOT NULL CHECK (signal_count BETWEEN 0 AND 10),
    extractor_provider VARCHAR(40) NOT NULL,
    extractor_model VARCHAR(120) NOT NULL,
    extractor_version VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (journal_entry_id, content_version, taxonomy_version)
);
CREATE INDEX idx_entry_pattern_extractions_user ON entry_pattern_extractions(user_id, created_at DESC);

-- A sentence of an entry that matched a pattern code. The quote is the user's own text and is encrypted.
CREATE TABLE entry_pattern_signals (
    id UUID PRIMARY KEY,
    extraction_id UUID NOT NULL REFERENCES entry_pattern_extractions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    journal_entry_id UUID NOT NULL REFERENCES journal_entries(id) ON DELETE CASCADE,
    pattern_code VARCHAR(48) NOT NULL CHECK (pattern_code ~ '^[A-Z][A-Z0-9_]{0,47}$'),
    confidence NUMERIC(5,4) NOT NULL CHECK (confidence BETWEEN 0 AND 1),
    encrypted_quote BYTEA NOT NULL,
    quote_iv BYTEA NOT NULL,
    quote_wrapped_key BYTEA NOT NULL,
    quote_key_version VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (extraction_id, pattern_code)
);
CREATE INDEX idx_entry_pattern_signals_user_code ON entry_pattern_signals(user_id, pattern_code);
CREATE INDEX idx_entry_pattern_signals_entry ON entry_pattern_signals(journal_entry_id);

-- A card shown to the user for one pattern code over a 30-day window. Owned by the insight module.
CREATE TABLE pattern_recognitions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    pattern_code VARCHAR(48) NOT NULL CHECK (pattern_code ~ '^[A-Z][A-Z0-9_]{0,47}$'),
    taxonomy_version VARCHAR(16) NOT NULL CHECK (taxonomy_version ~ '^[a-z0-9.-]{1,16}$'),
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL CHECK (status IN ('ACTIVE', 'SUPERSEDED', 'HIDDEN')),
    user_feedback VARCHAR(24) CHECK (user_feedback IN ('CONFIRMED', 'NOT_REALLY')),
    feedback_reason VARCHAR(32) CHECK (feedback_reason IN ('NOT_LIKE_ME', 'DONT_WANT_TO_SEE', 'WRONG_QUOTE')),
    feedback_at TIMESTAMPTZ,
    entry_count INTEGER NOT NULL CHECK (entry_count >= 0),
    week_count INTEGER NOT NULL CHECK (week_count >= 0),
    average_confidence NUMERIC(5,4) NOT NULL CHECK (average_confidence BETWEEN 0 AND 1),
    knowledge_version_id UUID REFERENCES knowledge_versions(id) ON DELETE SET NULL,
    encrypted_narrative BYTEA NOT NULL,
    narrative_iv BYTEA NOT NULL,
    narrative_wrapped_key BYTEA NOT NULL,
    narrative_key_version VARCHAR(32) NOT NULL,
    generated_by VARCHAR(24) NOT NULL CHECK (generated_by IN ('TEMPLATE', 'AI_ASSISTED')),
    model VARCHAR(120),
    plan_goal_id UUID REFERENCES selfcare_goals(id) ON DELETE SET NULL,
    plan_started_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    superseded_at TIMESTAMPTZ,
    CHECK (period_start <= period_end),
    CHECK ((user_feedback IS NULL) = (feedback_at IS NULL)),
    CHECK (feedback_reason IS NULL OR user_feedback = 'NOT_REALLY'),
    CHECK (plan_goal_id IS NULL OR plan_started_at IS NOT NULL),
    CHECK ((generated_by = 'AI_ASSISTED') = (model IS NOT NULL))
);
-- At most one live card per code; feedback and plan stay on that row across refreshes.
CREATE UNIQUE INDEX uq_pattern_recognitions_active ON pattern_recognitions(user_id, pattern_code)
    WHERE status = 'ACTIVE';
CREATE INDEX idx_pattern_recognitions_user ON pattern_recognitions(user_id, created_at DESC, id DESC);

-- Signals a card is based on. Deleting an entry removes its signals and with them this evidence.
CREATE TABLE pattern_recognition_evidence (
    recognition_id UUID NOT NULL REFERENCES pattern_recognitions(id) ON DELETE CASCADE,
    signal_id UUID NOT NULL REFERENCES entry_pattern_signals(id) ON DELETE CASCADE,
    shown_as_quote BOOLEAN NOT NULL DEFAULT false,
    PRIMARY KEY (recognition_id, signal_id)
);
CREATE INDEX idx_pattern_recognition_evidence_signal ON pattern_recognition_evidence(signal_id);

-- ADR-0008: the backend is the only data API.
REVOKE ALL PRIVILEGES ON TABLE entry_pattern_extractions, entry_pattern_signals, pattern_recognitions,
    pattern_recognition_evidence FROM PUBLIC;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
        REVOKE ALL PRIVILEGES ON TABLE entry_pattern_extractions, entry_pattern_signals, pattern_recognitions,
            pattern_recognition_evidence FROM anon;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        REVOKE ALL PRIVILEGES ON TABLE entry_pattern_extractions, entry_pattern_signals, pattern_recognitions,
            pattern_recognition_evidence FROM authenticated;
    END IF;
END $$;
ALTER TABLE entry_pattern_extractions ENABLE ROW LEVEL SECURITY;
ALTER TABLE entry_pattern_signals ENABLE ROW LEVEL SECURITY;
ALTER TABLE pattern_recognitions ENABLE ROW LEVEL SECURITY;
ALTER TABLE pattern_recognition_evidence ENABLE ROW LEVEL SECURITY;
