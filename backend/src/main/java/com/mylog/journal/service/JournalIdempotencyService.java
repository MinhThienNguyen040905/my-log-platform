package com.mylog.journal.service;

import com.mylog.common.api.ApiErrorCodes;
import com.mylog.common.exception.BadRequestException;
import com.mylog.common.exception.ConflictException;
import com.mylog.journal.repository.JournalIdempotencyRepository;
import com.mylog.journal.repository.JournalIdempotencyRepository.StoredRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JournalIdempotencyService {

    private static final String METHOD = "POST";
    private static final String PATH = "/api/v1/journals";
    private static final Duration RETENTION = Duration.ofHours(24);

    private final JournalIdempotencyRepository repository;

    public JournalIdempotencyService(JournalIdempotencyRepository repository) {
        this.repository = repository;
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
        if (repository.insertProcessing(
                recordId, userId, key, METHOD, PATH, requestHash, now, now.plus(RETENTION))) {
            return new Claim(recordId, null, true);
        }

        StoredRequest stored = repository.find(userId, key, METHOD, PATH)
                .orElseThrow(() -> new ConflictException(
                        ApiErrorCodes.IDEMPOTENCY_REQUEST_IN_PROGRESS,
                        "Idempotency request could not be resolved"));
        if (!requestHash.equals(stored.requestHash())) {
            throw new ConflictException(
                    ApiErrorCodes.IDEMPOTENCY_KEY_REUSED,
                    "Idempotency-Key was already used for a different request");
        }
        if (!"COMPLETED".equals(stored.status()) || stored.resourceId() == null) {
            throw new ConflictException(
                    ApiErrorCodes.IDEMPOTENCY_REQUEST_IN_PROGRESS,
                    "An idempotent request with this key is already in progress");
        }
        return new Claim(null, stored.resourceId(), false);
    }

    public void complete(UUID recordId, UUID resourceId, Instant now) {
        if (recordId == null) {
            return;
        }
        repository.complete(recordId, resourceId, now);
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
