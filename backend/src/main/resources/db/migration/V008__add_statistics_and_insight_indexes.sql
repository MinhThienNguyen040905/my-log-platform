CREATE INDEX idx_daily_user_statistics_user_date
    ON daily_user_statistics (user_id, entry_date DESC);

CREATE INDEX idx_daily_emotion_statistics_user_date
    ON daily_emotion_statistics (user_id, entry_date DESC, emotion_type);

CREATE INDEX idx_insights_user_status_period
    ON insights (user_id, status, period_end DESC);

CREATE INDEX idx_insight_evidence_insight
    ON insight_evidence (insight_id);

CREATE INDEX idx_suggested_actions_user_status
    ON suggested_actions (user_id, status, created_at DESC);

CREATE INDEX idx_feedback_user_updated
    ON feedback (user_id, updated_at DESC);
