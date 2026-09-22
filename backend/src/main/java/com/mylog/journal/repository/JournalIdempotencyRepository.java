package com.mylog.journal.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JournalIdempotencyRepository {

    private final JdbcTemplate jdbc;

    public JournalIdempotencyRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean insertProcessing(
            UUID id, UUID userId, String key, String method, String path,
            String requestHash, Instant createdAt, Instant expiresAt) {
        return jdbc.update("""
                INSERT INTO idempotency_records (
                    id, user_id, idempotency_key, request_method, request_path,
                    request_hash, status, created_at, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, 'PROCESSING', ?, ?)
                ON CONFLICT (user_id, request_method, request_path, idempotency_key)
                DO NOTHING
                """, id, userId, key, method, path, requestHash,
                Timestamp.from(createdAt), Timestamp.from(expiresAt)) == 1;
    }

    public Optional<StoredRequest> find(UUID userId, String key, String method, String path) {
        return jdbc.query("""
                SELECT request_hash, status, resource_id
                FROM idempotency_records
                WHERE user_id = ? AND request_method = ? AND request_path = ? AND idempotency_key = ?
                """, (resultSet, rowNumber) -> new StoredRequest(
                        resultSet.getString("request_hash"),
                        resultSet.getString("status"),
                        resultSet.getObject("resource_id", UUID.class)),
                userId, method, path, key).stream().findFirst();
    }

    public void complete(UUID recordId, UUID resourceId, Instant completedAt) {
        jdbc.update("""
                UPDATE idempotency_records
                SET status = 'COMPLETED', response_status = 201,
                    resource_id = ?, completed_at = ?
                WHERE id = ?
                """, resourceId, Timestamp.from(completedAt), recordId);
    }

    public record StoredRequest(String requestHash, String status, UUID resourceId) {}
}
