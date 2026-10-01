CREATE TABLE selfcare_goals (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category VARCHAR(32) NOT NULL CHECK (category IN ('SLEEP','MINDFULNESS','EXERCISE','SOCIAL','CUSTOM')),
    encrypted_title BYTEA NOT NULL,
    title_iv BYTEA NOT NULL,
    title_wrapped_key BYTEA NOT NULL,
    title_key_version VARCHAR(32) NOT NULL,
    encrypted_description BYTEA,
    description_iv BYTEA,
    description_wrapped_key BYTEA,
    description_key_version VARCHAR(32),
    status VARCHAR(24) NOT NULL CHECK (status IN ('ACTIVE','PAUSED','COMPLETED','ARCHIVED')),
    start_date DATE,
    target_date DATE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    row_version BIGINT NOT NULL DEFAULT 0 CHECK (row_version >= 0),
    UNIQUE(id, user_id),
    CHECK (target_date IS NULL OR start_date IS NULL OR target_date >= start_date),
    CHECK ((encrypted_description IS NULL AND description_iv IS NULL AND description_wrapped_key IS NULL AND description_key_version IS NULL)
        OR (encrypted_description IS NOT NULL AND description_iv IS NOT NULL AND description_wrapped_key IS NOT NULL AND description_key_version IS NOT NULL))
);
CREATE INDEX idx_selfcare_goals_user ON selfcare_goals(user_id, created_at DESC, id DESC);

CREATE TABLE habits (
    id UUID PRIMARY KEY,
    goal_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    encrypted_title BYTEA NOT NULL,
    title_iv BYTEA NOT NULL,
    title_wrapped_key BYTEA NOT NULL,
    title_key_version VARCHAR(32) NOT NULL,
    target_value NUMERIC(10,2) NOT NULL CHECK (target_value > 0),
    unit VARCHAR(32) NOT NULL,
    frequency_type VARCHAR(24) NOT NULL CHECK (frequency_type IN ('DAILY','WEEKLY')),
    frequency_config JSONB NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL CHECK (status IN ('ACTIVE','PAUSED','ARCHIVED')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0 CHECK (row_version >= 0),
    UNIQUE(id, user_id),
    FOREIGN KEY (goal_id, user_id) REFERENCES selfcare_goals(id, user_id) ON DELETE CASCADE
);
CREATE INDEX idx_habits_goal_user ON habits(goal_id, user_id);

CREATE TABLE habit_completions (
    id UUID PRIMARY KEY,
    habit_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    local_date DATE NOT NULL,
    value NUMERIC(10,2) NOT NULL CHECK (value > 0),
    source VARCHAR(24) NOT NULL CHECK (source IN ('USER','IMPORT')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE(habit_id, local_date),
    FOREIGN KEY (habit_id, user_id) REFERENCES habits(id, user_id) ON DELETE CASCADE
);
CREATE INDEX idx_habit_completions_user_date ON habit_completions(user_id, local_date DESC);
