package com.mylog.journal.application;

import com.mylog.shared.api.ApiErrorCodes;
import com.mylog.shared.exception.BadRequestException;
import com.mylog.shared.exception.ConflictException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JournalIdempotencyService {

    private static final String METHOD = "POST";
    private static final String PATH = "/api/v1/journals";
    private static final Duration RETENTION = Duration.ofHours(24);

    private final JdbcTemplate jdbcTemplate;

    public JournalIdempotencyService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Claim claim(UUID userId, String key, String requestHash, Instant now) {
        if (key == null) {
            return Claim.untracked();
        }
        if (key.isBlank() || key.length() > 100) {
            throw new BadRequestException(
                    ApiErrorCodes.INVALID_IDEMPOTENCY_KEY,
                    "Idempotency-Key must contain between 1 and 100 characters");
        }

        UUID recordId = UUID.randomUUID();
        int inserted = jdbcTemplate.update(
                """
                INSERT INTO idempotency_records (
                    id, user_id, idempotency_key, request_method, request_path,
                    request_hash, status, created_at, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, 'PROCESSING', ?, ?)
                ON CONFLICT (user_id, request_method, request_path, idempotency_key)
                DO NOTHING
                """,
                recordId,
                userId,
                key,
                METHOD,
                PATH,
                requestHash,
                Timestamp.from(now),
                Timestamp.from(now.plus(RETENTION)));
        if (inserted == 1) {
            return new Claim(recordId, null, true);
        }

        return jdbcTemplate.queryForObject(
                """
                SELECT request_hash, status, resource_id
                FROM idempotency_records
                WHERE user_id = ? AND request_method = ? AND request_path = ? AND idempotency_key = ?
                """,
                (resultSet, rowNumber) -> {
                    if (!requestHash.equals(resultSet.getString("request_hash"))) {
                        throw new ConflictException(
                                ApiErrorCodes.IDEMPOTENCY_KEY_REUSED,
                                "Idempotency-Key was already used for a different request");
                    }
                    UUID resourceId = resultSet.getObject("resource_id", UUID.class);
                    if (!"COMPLETED".equals(resultSet.getString("status")) || resourceId == null) {
                        throw new ConflictException(
                                ApiErrorCodes.IDEMPOTENCY_REQUEST_IN_PROGRESS,
                                "An idempotent request with this key is already in progress");
                    }
                    return new Claim(null, resourceId, false);
                },
                userId,
                METHOD,
                PATH,
                key);
    }

    public void complete(UUID recordId, UUID resourceId, Instant now) {
        if (recordId == null) {
            return;
        }
        jdbcTemplate.update(
                """
                UPDATE idempotency_records
                SET status = 'COMPLETED', response_status = 201,
                    resource_id = ?, completed_at = ?
                WHERE id = ?
                """,
                resourceId,
                Timestamp.from(now),
                recordId);
    }

    public record Claim(UUID recordId, UUID existingResourceId, boolean tracked) {
        static Claim untracked() {
            return new Claim(null, null, false);
        }

        public boolean isReplay() {
            return existingResourceId != null;
        }
    }
}
