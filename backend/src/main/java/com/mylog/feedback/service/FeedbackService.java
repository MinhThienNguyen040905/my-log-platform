package com.mylog.feedback.service;

import com.mylog.common.api.ApiErrorCodes;
import com.mylog.common.exception.BadRequestException;
import com.mylog.common.exception.ResourceNotFoundException;
import com.mylog.feedback.repository.FeedbackRepository;
import java.time.Clock;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedbackService {

    private static final Set<String> TARGET_TYPES = Set.of("REFLECTION", "ACTION", "INSIGHT");
    private static final Set<String> VALUES = Set.of("HELPFUL", "NOT_HELPFUL");

    private final FeedbackRepository repository;
    private final Clock clock;

    public FeedbackService(FeedbackRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public FeedbackView put(UUID userId, String targetTypeValue, UUID targetId, String feedbackValue) {
        String targetType = normalize(targetTypeValue);
        String value = normalize(feedbackValue);
        if (!TARGET_TYPES.contains(targetType) || !VALUES.contains(value)) {
            throw new BadRequestException(ApiErrorCodes.INVALID_REQUEST, "Feedback target type or value is invalid");
        }
        if (!repository.targetBelongsToUser(userId, targetType, targetId)) {
            throw new ResourceNotFoundException(ApiErrorCodes.RESOURCE_NOT_FOUND, "Feedback target does not exist");
        }
        var row = repository.upsert(userId, targetType, targetId, value, clock.instant());
        return new FeedbackView(
                row.id(), row.targetType(), row.targetId(), row.value(), row.createdAt(), row.updatedAt());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
