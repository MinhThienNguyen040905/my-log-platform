package com.mylog.journal.service;

import com.mylog.common.api.ApiErrorCodes;
import com.mylog.common.exception.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JournalCursorCodec {

    public String encode(Instant createdAt, UUID id) {
        String value = createdAt + "|" + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public Cursor decode(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return new Cursor(null, null);
        }
        try {
            String value = new String(
                    Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            String[] parts = value.split("\\|", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Cursor has an invalid shape");
            }
            return new Cursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            throw new BadRequestException(ApiErrorCodes.INVALID_CURSOR, "Journal cursor is invalid");
        }
    }

    public record Cursor(Instant createdAt, UUID id) {}
}
