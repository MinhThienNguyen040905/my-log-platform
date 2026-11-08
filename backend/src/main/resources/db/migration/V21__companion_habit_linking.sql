-- Intentional trade-off (see COMPANION_CHATBOT_PLAN.md stage 5): enforce allowed tool codes at the DB level;
-- adding a new tool code in journal requires a migration expanding this CHECK constraint.
ALTER TABLE habits
    ADD COLUMN tool_code VARCHAR(32)
    CHECK (tool_code IS NULL OR tool_code IN ('BREATHING_2MIN', 'AMBIENT', 'VENT_PROMPT'));

CREATE TABLE companion_habit_preferences (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    last_suggested_date DATE,
    dismissed_until TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL
);

REVOKE ALL PRIVILEGES ON TABLE public.companion_habit_preferences FROM PUBLIC;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='anon') THEN
        EXECUTE 'REVOKE ALL PRIVILEGES ON TABLE public.companion_habit_preferences FROM anon';
    END IF;
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='authenticated') THEN
        EXECUTE 'REVOKE ALL PRIVILEGES ON TABLE public.companion_habit_preferences FROM authenticated';
    END IF;
END $$;

ALTER TABLE public.companion_habit_preferences ENABLE ROW LEVEL SECURITY;
