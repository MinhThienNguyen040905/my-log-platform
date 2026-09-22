package com.mylog.journal.service;

import com.mylog.common.api.ApiErrorCodes;
import com.mylog.common.exception.BadRequestException;
import com.mylog.journal.entity.ContentFormat;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class JournalEntryValidator {

    ZoneId validZone(String timezone) {
        try {
            if (timezone == null || timezone.isBlank()) {
                throw new DateTimeException("Timezone is blank");
            }
            return ZoneId.of(timezone);
        } catch (DateTimeException exception) {
            throw new BadRequestException(ApiErrorCodes.INVALID_REQUEST, "timezoneAtEntry is invalid");
        }
    }

    void validateContent(String contentText, Map<String, Object> contentJson, ContentFormat contentFormat) {
        if (contentText == null || contentText.isBlank()) {
            throw new BadRequestException(
                    ApiErrorCodes.INVALID_REQUEST, "Journal content must contain meaningful text");
        }
        if (contentFormat == null) {
            throw new BadRequestException(ApiErrorCodes.INVALID_REQUEST, "contentFormat is required");
        }
        if (contentFormat == ContentFormat.TIPTAP_JSON && contentJson == null) {
            throw new BadRequestException(
                    ApiErrorCodes.INVALID_REQUEST, "contentJson is required for TIPTAP_JSON content");
        }
    }

    void validateScore(Integer score, String field) {
        if (score == null || score < 1 || score > 10) {
            throw new BadRequestException(ApiErrorCodes.INVALID_REQUEST, field + " must be between 1 and 10");
        }
    }

    void validateNullableScore(Integer score, String field) {
        if (score != null) {
            validateScore(score, field);
        }
    }

    <T> T requiredMutation(UpdateJournalCommand.Value<T> value, T current, String field) {
        if (!value.supplied()) {
            return current;
        }
        if (value.value() == null) {
            throw new BadRequestException(ApiErrorCodes.INVALID_REQUEST, field + " must not be null");
        }
        return value.value();
    }

    String normalizeTitle(String title) {
        return title == null || title.isBlank() ? null : title.trim();
    }

    Map<String, Object> copyJson(Map<String, Object> value) {
        return value == null ? null : new LinkedHashMap<>(value);
    }
}
