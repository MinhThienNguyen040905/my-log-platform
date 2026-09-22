package com.mylog.journal.service;

import com.mylog.journal.entity.ContentFormat;
import com.mylog.journal.entity.JournalEntry;
import com.mylog.journal.repository.JournalEntryRepository;
import com.mylog.common.api.ApiErrorCodes;
import com.mylog.common.exception.BadRequestException;
import com.mylog.common.exception.ConflictException;
import com.mylog.common.exception.ResourceNotFoundException;
import com.mylog.common.messaging.MessagingTopology;
import com.mylog.common.outbox.OutboxWriter;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JournalService {

    private static final LocalDate MIN_FILTER_DATE = LocalDate.of(1, 1, 1);
    private static final LocalDate MAX_FILTER_DATE = LocalDate.of(9999, 12, 31);
    private static final Instant FIRST_PAGE_CURSOR = Instant.parse("9999-12-31T23:59:59.999999Z");
    private static final UUID MAX_UUID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

    private final JournalEntryRepository repository;
    private final JournalCursorCodec cursorCodec;
    private final JournalIdempotencyService idempotencyService;
    private final JournalRequestHasher requestHasher;
    private final OutboxWriter outboxWriter;
    private final Clock clock;

    public JournalService(
            JournalEntryRepository repository,
            JournalCursorCodec cursorCodec,
            JournalIdempotencyService idempotencyService,
            JournalRequestHasher requestHasher,
            OutboxWriter outboxWriter,
            Clock clock) {
        this.repository = repository;
        this.cursorCodec = cursorCodec;
        this.idempotencyService = idempotencyService;
        this.requestHasher = requestHasher;
        this.outboxWriter = outboxWriter;
        this.clock = clock;
    }

    @Transactional
    public JournalEntry create(UUID userId, CreateJournalCommand command, String idempotencyKey) {
        Instant now = clock.instant();
        JournalIdempotencyService.Claim claim = idempotencyService.claim(
                userId, idempotencyKey, requestHasher.hash(command), now);
        if (claim.isReplay()) {
            return owned(userId, claim.existingResourceId());
        }

        Instant occurredAt = command.occurredAt() == null ? now : command.occurredAt();
        ZoneId zone = validZone(command.timezoneAtEntry());
        validateContent(command.contentText(), command.contentJson(), command.contentFormat());

        JournalEntry entry = new JournalEntry(
                UUID.randomUUID(),
                userId,
                normalizeTitle(command.title()),
                command.contentText(),
                copyJson(command.contentJson()),
                command.contentFormat(),
                (short) command.moodScore(),
                toShort(command.stressScore()),
                toShort(command.energyScore()),
                occurredAt,
                occurredAt.atZone(zone).toLocalDate(),
                zone.getId(),
                command.favorite(),
                now);
        repository.saveAndFlush(entry);
        appendJournalEvent(entry, MessagingTopology.JOURNAL_CREATED);
        appendAnalysisRequested(entry);
        idempotencyService.complete(claim.recordId(), entry.getId(), now);
        return entry;
    }

    @Transactional(readOnly = true)
    public JournalEntry get(UUID userId, UUID journalId) {
        return owned(userId, journalId);
    }

    @Transactional(readOnly = true)
    public JournalPage history(
            UUID userId,
            String encodedCursor,
            int limit,
            LocalDate fromDate,
            LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException(
                    ApiErrorCodes.INVALID_REQUEST, "from date must not be after to date");
        }
        JournalCursorCodec.Cursor cursor = cursorCodec.decode(encodedCursor);
        List<JournalEntry> result = repository.findHistory(
                userId,
                fromDate == null ? MIN_FILTER_DATE : fromDate,
                toDate == null ? MAX_FILTER_DATE : toDate,
                cursor.createdAt() == null ? FIRST_PAGE_CURSOR : cursor.createdAt(),
                cursor.id() == null ? MAX_UUID : cursor.id(),
                PageRequest.of(0, limit + 1));
        boolean hasMore = result.size() > limit;
        List<JournalEntry> items = hasMore ? List.copyOf(result.subList(0, limit)) : List.copyOf(result);
        String nextCursor = hasMore
                ? cursorCodec.encode(
                        items.get(items.size() - 1).getCreatedAt(),
                        items.get(items.size() - 1).getId())
                : null;
        return new JournalPage(items, nextCursor, hasMore);
    }

    @Transactional
    public JournalEntry update(UUID userId, UUID journalId, UpdateJournalCommand command) {
        JournalEntry entry = owned(userId, journalId);
        if (entry.getVersion() != command.version()) {
            throw versionConflict();
        }

        String title = command.title().supplied()
                ? normalizeTitle(command.title().value())
                : entry.getTitle();
        String contentText = requiredMutation(
                command.contentText(), entry.getContentText(), "contentText");
        ContentFormat contentFormat = requiredMutation(
                command.contentFormat(), entry.getContentFormat(), "contentFormat");
        Integer moodScore = requiredMutation(
                command.moodScore(), (int) entry.getMoodScore(), "moodScore");
        Instant occurredAt = requiredMutation(
                command.occurredAt(), entry.getOccurredAt(), "occurredAt");
        String timezone = requiredMutation(
                command.timezoneAtEntry(), entry.getTimezoneAtEntry(), "timezoneAtEntry");
        ZoneId zone = validZone(timezone);
        Map<String, Object> contentJson = command.contentJson().supplied()
                ? copyJson(command.contentJson().value())
                : entry.getContentJson();
        Integer stressScore = command.stressScore().supplied()
                ? command.stressScore().value()
                : toInteger(entry.getStressScore());
        Integer energyScore = command.energyScore().supplied()
                ? command.energyScore().value()
                : toInteger(entry.getEnergyScore());
        boolean favorite = command.favorite().supplied()
                ? requiredMutation(command.favorite(), entry.isFavorite(), "favorite")
                : entry.isFavorite();

        validateContent(contentText, contentJson, contentFormat);
        validateScore(moodScore, "moodScore");
        validateNullableScore(stressScore, "stressScore");
        validateNullableScore(energyScore, "energyScore");

        boolean analysisInputChanged = entry.update(
                title,
                contentText,
                contentJson,
                contentFormat,
                moodScore.shortValue(),
                toShort(stressScore),
                toShort(energyScore),
                occurredAt,
                occurredAt.atZone(zone).toLocalDate(),
                zone.getId(),
                favorite,
                clock.instant());
        JournalEntry saved = repository.saveAndFlush(entry);
        appendJournalEvent(saved, MessagingTopology.JOURNAL_UPDATED);
        if (analysisInputChanged) {
            appendAnalysisRequested(saved);
        }
        return saved;
    }

    @Transactional
    public void delete(UUID userId, UUID journalId) {
        JournalEntry entry = owned(userId, journalId);
        entry.softDelete(clock.instant());
        repository.saveAndFlush(entry);
        appendJournalEvent(entry, MessagingTopology.JOURNAL_DELETED);
    }

    private JournalEntry owned(UUID userId, UUID journalId) {
        return repository.findByIdAndUserIdAndDeletedAtIsNull(journalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ApiErrorCodes.JOURNAL_NOT_FOUND, "Journal entry does not exist"));
    }

    private ZoneId validZone(String timezone) {
        try {
            if (timezone == null || timezone.isBlank()) {
                throw new DateTimeException("Timezone is blank");
            }
            return ZoneId.of(timezone);
        } catch (DateTimeException exception) {
            throw new BadRequestException(ApiErrorCodes.INVALID_REQUEST, "timezoneAtEntry is invalid");
        }
    }

    private void validateContent(
            String contentText,
            Map<String, Object> contentJson,
            ContentFormat contentFormat) {
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

    private void validateScore(Integer score, String field) {
        if (score == null || score < 1 || score > 10) {
            throw new BadRequestException(ApiErrorCodes.INVALID_REQUEST, field + " must be between 1 and 10");
        }
    }

    private void validateNullableScore(Integer score, String field) {
        if (score != null) {
            validateScore(score, field);
        }
    }

    private <T> T requiredMutation(UpdateJournalCommand.Value<T> value, T current, String field) {
        if (!value.supplied()) {
            return current;
        }
        if (value.value() == null) {
            throw new BadRequestException(ApiErrorCodes.INVALID_REQUEST, field + " must not be null");
        }
        return value.value();
    }

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }
        return title.trim();
    }

    private Map<String, Object> copyJson(Map<String, Object> value) {
        return value == null ? null : new LinkedHashMap<>(value);
    }

    private Short toShort(Integer value) {
        return value == null ? null : value.shortValue();
    }

    private Integer toInteger(Short value) {
        return value == null ? null : value.intValue();
    }

    private ConflictException versionConflict() {
        return new ConflictException(
                ApiErrorCodes.JOURNAL_VERSION_CONFLICT,
                "Journal entry was modified by another request");
    }

    private void appendJournalEvent(JournalEntry entry, String eventType) {
        outboxWriter.append(
                "JOURNAL",
                entry.getId(),
                eventType,
                1,
                eventPayload(entry));
    }

    private void appendAnalysisRequested(JournalEntry entry) {
        outboxWriter.append(
                "JOURNAL",
                entry.getId(),
                MessagingTopology.JOURNAL_ANALYSIS_REQUESTED,
                1,
                eventPayload(entry));
    }

    private Map<String, Object> eventPayload(JournalEntry entry) {
        return Map.of(
                "journalId", entry.getId().toString(),
                "userId", entry.getUserId().toString(),
                "journalVersion", entry.getJournalVersion());
    }
}
