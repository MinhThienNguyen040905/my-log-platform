package com.mylog.feedback.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FeedbackRepository {

    private final JdbcTemplate jdbc;

    public FeedbackRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean targetBelongsToUser(UUID userId, String targetType, UUID targetId) {
        String sql = switch (targetType) {
            case "REFLECTION" -> "SELECT EXISTS (SELECT 1 FROM reflection_questions WHERE id = ? AND user_id = ?)";
            case "ACTION" -> "SELECT EXISTS (SELECT 1 FROM suggested_actions WHERE id = ? AND user_id = ?)";
            case "INSIGHT" -> "SELECT EXISTS (SELECT 1 FROM insights WHERE id = ? AND user_id = ?)";
            default -> throw new IllegalArgumentException("Unsupported target type");
        };
        return Boolean.TRUE.equals(jdbc.queryForObject(sql, Boolean.class, targetId, userId));
    }

    public FeedbackRow upsert(
            UUID userId, String targetType, UUID targetId, String value, Instant now) {
        return jdbc.queryForObject("""
                INSERT INTO feedback (id, user_id, target_type, target_id, value, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (user_id, target_type, target_id) DO UPDATE
                SET value = EXCLUDED.value, updated_at = EXCLUDED.updated_at
                RETURNING id, target_type, target_id, value, created_at, updated_at
                """, (rs, row) -> new FeedbackRow(
                        rs.getObject("id", UUID.class), rs.getString("target_type"),
                        rs.getObject("target_id", UUID.class), rs.getString("value"),
                        rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant()),
                UUID.randomUUID(), userId, targetType, targetId, value,
                Timestamp.from(now), Timestamp.from(now));
    }

    public record FeedbackRow(
            UUID id, String targetType, UUID targetId, String value, Instant createdAt, Instant updatedAt) {}
}
