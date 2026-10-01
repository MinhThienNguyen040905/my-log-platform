package com.mylog.journal.application;

import com.mylog.journal.application.command.CreateJournalEntryCommand;
import com.mylog.journal.application.command.UpdateJournalEntryCommand;
import com.mylog.journal.application.query.JournalEntryView;
import com.mylog.journal.application.query.JournalPage;
import com.mylog.journal.application.query.JournalSummaryView;
import com.mylog.platform.id.IdGenerator;
import com.mylog.platform.idempotency.IdempotencyStore;
import com.mylog.platform.outbox.OutboxPublisher;
import com.mylog.platform.web.ConflictException;
import com.mylog.platform.web.InvalidRequestException;
import com.mylog.platform.web.ResourceNotFoundException;
import com.mylog.safety.application.SafetyDecision;
import com.mylog.safety.application.SafetyEventRecorder;
import com.mylog.safety.application.SafetyScreeningUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class JournalService {
    private final JournalStore store;
    private final JournalContentCipher cipher;
    private final SafetyScreeningUseCase screening;
    private final SafetyEventRecorder safetyEvents;
    private final OutboxPublisher outbox;
    private final IdempotencyStore idempotency;
    private final IdGenerator ids;
    private final Clock clock;
    private final ObjectMapper mapper;

    public JournalService(JournalStore store, JournalContentCipher cipher, SafetyScreeningUseCase screening,
                          SafetyEventRecorder safetyEvents, OutboxPublisher outbox, IdempotencyStore idempotency,
                          IdGenerator ids, Clock clock, ObjectMapper mapper) {
        this.store = store; this.cipher = cipher; this.screening = screening; this.safetyEvents = safetyEvents;
        this.outbox = outbox; this.idempotency = idempotency; this.ids = ids; this.clock = clock; this.mapper = mapper;
    }

    @Transactional
    public JournalEntryView create(UUID userId, CreateJournalEntryCommand command, String key) {
        if (key == null || !key.matches("[A-Za-z0-9._:-]{8,160}")) throw new InvalidRequestException();
        Instant now = clock.instant();
        Instant occurredAt = command.occurredAt() == null ? now : command.occurredAt();
        String timezone = timezone(command.timezone());
        validate(occurredAt, command.title(), command.location(), command.moodCode(), command.moodScore(),
                command.stressScore(), command.energyScore(), command.sleepMinutes());
        String plainText = TipTapContent.plainText(command.contentJson());
        var existing = idempotency.reserve(userId, "journal-create", key,
                cipher.lookupHash("journal-create:" + mapper.writeValueAsString(command)), now);
        if (existing.isPresent()) return get(userId, existing.get());
        UUID id = ids.next();
        String title = command.title() == null ? "" : command.title().trim();
        JournalPayload payload = new JournalPayload(1, title, command.contentJson(), plainText, command.location());
        SafetyDecision decision = screening.screen(title + "\n" + plainText);
        String status = decision.permitsOrdinaryAnalysis() ? "PENDING" : "BLOCKED_BY_SAFETY";
        JournalEntrySnapshot entry = new JournalEntrySnapshot(id, userId,
                cipher.encrypt(userId, id, mapper.writeValueAsString(payload)), occurredAt,
                occurredAt.atZone(ZoneId.of(timezone)).toLocalDate(), timezone, command.moodCode(),
                command.moodScore(), command.stressScore(), command.energyScore(), command.sleepMinutes(), false,
                "SAVED", decision.riskLevel().name(), status, 1, 0, now, now, null);
        store.create(entry);
        safetyEvents.record(userId, id, decision, now);
        publish(id, 1, decision, now);
        idempotency.complete(userId, "journal-create", key, id, 201, now);
        return view(entry);
    }

    @Transactional(readOnly = true)
    public JournalEntryView get(UUID userId, UUID entryId) { return view(require(userId, entryId)); }

    @Transactional(readOnly = true)
    public JournalPage list(UUID userId, LocalDate from, LocalDate to, UUID tagId, Boolean favorite,
                            String cursor, int pageSize) {
        if (pageSize < 1 || pageSize > 100 || from != null && to != null && from.isAfter(to))
            throw new InvalidRequestException();
        Cursor decoded = decode(cursor);
        List<JournalEntrySnapshot> entries = store.list(userId, from, to, tagId, favorite,
                decoded == null ? null : decoded.occurredAt(), decoded == null ? null : decoded.id(), pageSize + 1);
        boolean more = entries.size() > pageSize;
        List<JournalEntrySnapshot> page = more ? entries.subList(0, pageSize) : entries;
        String next = more ? encode(page.get(page.size() - 1)) : null;
        return new JournalPage(page.stream().map(this::summary).toList(), next);
    }

    @Transactional
    public JournalEntryView update(UUID userId, UUID entryId, UpdateJournalEntryCommand command, long expectedVersion) {
        JournalEntrySnapshot old = require(userId, entryId);
        if (old.rowVersion() != expectedVersion) throw new ConflictException("Nhật ký đã được cập nhật ở nơi khác.");
        JournalPayload previous = payload(old);
        String title = command.title() == null ? previous.title() : command.title().trim();
        JsonNode content = command.contentJson() == null ? previous.contentJson() : command.contentJson();
        String plainText = TipTapContent.plainText(content);
        String location = command.location() == null ? previous.location() : command.location();
        Instant occurredAt = command.occurredAt() == null ? old.occurredAt() : command.occurredAt();
        String timezone = command.timezone() == null ? old.timezone() : timezone(command.timezone());
        String moodCode = command.moodCode() == null ? old.moodCode() : command.moodCode();
        BigDecimal mood = command.moodScore() == null ? old.moodScore() : command.moodScore();
        BigDecimal stress = command.stressScore() == null ? old.stressScore() : command.stressScore();
        BigDecimal energy = command.energyScore() == null ? old.energyScore() : command.energyScore();
        Integer sleep = command.sleepMinutes() == null ? old.sleepMinutes() : command.sleepMinutes();
        validate(occurredAt, title, location, moodCode, mood, stress, energy, sleep);
        SafetyDecision decision = screening.screen(title + "\n" + plainText);
        Instant now = clock.instant();
        JournalEntrySnapshot next = new JournalEntrySnapshot(entryId, userId,
                cipher.encrypt(userId, entryId, mapper.writeValueAsString(new JournalPayload(1, title, content, plainText, location))),
                occurredAt, occurredAt.atZone(ZoneId.of(timezone)).toLocalDate(), timezone, moodCode,
                mood, stress, energy, sleep, old.favorite(), old.entryStatus(), decision.riskLevel().name(),
                decision.permitsOrdinaryAnalysis() ? "ANALYSIS_OUTDATED" : "BLOCKED_BY_SAFETY",
                old.contentVersion() + 1, old.rowVersion() + 1, old.createdAt(), now, null);
        if (!store.update(next, expectedVersion)) throw new ConflictException("Nhật ký đã được cập nhật ở nơi khác.");
        safetyEvents.record(userId, entryId, decision, now);
        publish(entryId, next.contentVersion(), decision, now);
        return view(next);
    }

    @Transactional
    public void delete(UUID userId, UUID entryId, long expectedVersion) {
        JournalEntrySnapshot old = require(userId, entryId);
        if (!store.softDelete(userId, entryId, expectedVersion, clock.instant())) {
            throw new ConflictException("Nhật ký đã được cập nhật ở nơi khác.");
        }
        outbox.publish("JOURNAL_ENTRY", entryId, "JournalEntryDeleted", old.contentVersion(), clock.instant());
    }

    @Transactional
    public JournalEntryView favorite(UUID userId, UUID entryId, boolean favorite) {
        if (!store.setFavorite(userId, entryId, favorite, clock.instant())) throw notFound();
        return get(userId, entryId);
    }

    private void publish(UUID id, int contentVersion, SafetyDecision decision, Instant now) {
        if (decision.permitsOrdinaryAnalysis())
            outbox.publish("JOURNAL_ENTRY", id, "JournalEntrySubmitted", contentVersion, now);
        else if ("FAIL_SAFE".equals(decision.decision()))
            outbox.publish("JOURNAL_ENTRY", id, "SafetyRescreenRequested", contentVersion, now);
    }

    private JournalEntrySnapshot require(UUID userId, UUID entryId) {
        return store.find(userId, entryId).orElseThrow(JournalService::notFound);
    }

    private JournalPayload payload(JournalEntrySnapshot entry) {
        return mapper.readValue(cipher.decrypt(entry.userId(), entry.id(), entry.payload()), JournalPayload.class);
    }

    private JournalEntryView view(JournalEntrySnapshot entry) {
        JournalPayload data = payload(entry);
        return new JournalEntryView(entry.id(), data.title(), data.contentJson(), data.location(),
                entry.occurredAt(), entry.localDate(), entry.timezone(), entry.moodCode(), entry.moodScore(),
                entry.stressScore(), entry.energyScore(), entry.sleepMinutes(), entry.favorite(),
                entry.entryStatus(), entry.riskLevel(), entry.analysisStatus(), entry.contentVersion(),
                entry.rowVersion(), entry.createdAt(), entry.updatedAt());
    }

    private JournalSummaryView summary(JournalEntrySnapshot entry) {
        return new JournalSummaryView(entry.id(), payload(entry).title(), entry.occurredAt(), entry.localDate(),
                entry.moodCode(), entry.favorite(), entry.riskLevel(), entry.analysisStatus(), entry.rowVersion());
    }

    private static String timezone(String value) {
        if (value == null || value.length() > 64) throw new InvalidRequestException();
        try { return ZoneId.of(value).getId(); }
        catch (RuntimeException e) { throw new InvalidRequestException(); }
    }

    private void validate(Instant occurredAt, String title, String location, String moodCode,
                          BigDecimal mood, BigDecimal stress, BigDecimal energy, Integer sleep) {
        if (occurredAt == null || occurredAt.isAfter(clock.instant().plusSeconds(86400))
                || title != null && title.length() > 160 || location != null && location.length() > 160
                || moodCode != null && !moodCode.matches("[a-z0-9-]{1,32}")
                || !score(mood) || !score(stress) || !score(energy)
                || sleep != null && (sleep < 0 || sleep > 1440)) throw new InvalidRequestException();
    }

    private static boolean score(BigDecimal value) {
        return value == null || value.scale() <= 1 && value.compareTo(BigDecimal.ONE) >= 0
                && value.compareTo(BigDecimal.TEN) <= 0;
    }

    private static ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("Không tìm thấy nhật ký.");
    }

    private record Cursor(Instant occurredAt, UUID id) {}

    private static Cursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            String value = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = value.split("\\|", 2);
            if (parts.length != 2) throw new InvalidRequestException();
            return new Cursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (RuntimeException e) { throw new InvalidRequestException(); }
    }

    private static String encode(JournalEntrySnapshot entry) {
        String raw = entry.occurredAt() + "|" + entry.id();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
