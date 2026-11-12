package com.mylog.journal.application;

import com.mylog.journal.application.command.CreateJournalEntryCommand;
import com.mylog.journal.application.command.UpdateJournalEntryCommand;
import com.mylog.journal.application.query.DailyEntryCountView;
import com.mylog.journal.application.query.JournalEntryView;
import com.mylog.journal.application.query.JournalPage;
import com.mylog.journal.application.query.JournalSummaryView;
import com.mylog.journal.application.query.JournalTagView;
import com.mylog.journal.infrastructure.persistence.entity.JournalAsset;
import com.mylog.platform.id.IdGenerator;
import com.mylog.platform.idempotency.IdempotencyStore;
import com.mylog.platform.outbox.OutboxPublisher;
import com.mylog.platform.text.VietnameseStringUtils;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class JournalService implements JournalAnalysisAccess {
    private final JournalStore store;
    private final JournalTagStore tagStore;
    private final JournalContentCipher cipher;
    private final SafetyScreeningUseCase screening;
    private final SafetyEventRecorder safetyEvents;
    private final OutboxPublisher outbox;
    private final IdempotencyStore idempotency;
    private final IdGenerator ids;
    private final Clock clock;
    private final ObjectMapper mapper;
    private final JournalAssetStore assetStore;
    private final JournalAssetService assetService;

    public JournalService(JournalStore store, JournalTagStore tagStore, JournalContentCipher cipher,
                          SafetyScreeningUseCase screening, SafetyEventRecorder safetyEvents,
                          OutboxPublisher outbox, IdempotencyStore idempotency,
                          IdGenerator ids, Clock clock, ObjectMapper mapper,
                          JournalAssetStore assetStore, JournalAssetService assetService) {
        this.store = store; this.tagStore = tagStore; this.cipher = cipher; this.screening = screening;
        this.safetyEvents = safetyEvents; this.outbox = outbox; this.idempotency = idempotency;
        this.ids = ids; this.clock = clock; this.mapper = mapper;
        this.assetStore = assetStore; this.assetService = assetService;
    }

    @Transactional
    public JournalEntryView create(UUID userId, CreateJournalEntryCommand command, String key) {
        if (key == null || !key.matches("[A-Za-z0-9._:-]{8,160}")) throw new InvalidRequestException();
        Instant now = clock.instant();
        Instant occurredAt = command.occurredAt() == null ? now : command.occurredAt();
        String timezone = timezone(command.timezone());
        validate(occurredAt, command.title(), command.location(), command.moodCode(), command.moodScore(),
                command.stressScore(), command.energyScore(), command.sleepMinutes());

        List<UUID> assetIds = TipTapContent.extractAssetIds(command.contentJson());
        if (assetIds.size() > 20) {
            throw new InvalidRequestException("Mỗi bài viết chỉ được đính kèm tối đa 20 hình ảnh.");
        }

        String plainText = TipTapContent.plainText(command.contentJson());
        var existing = idempotency.reserve(userId, "journal-create", key,
                cipher.lookupHash("journal-create:" + mapper.writeValueAsString(command)), now);
        if (existing.isPresent()) return get(userId, existing.get());
        UUID id = ids.next();
        String title = command.title() == null ? "" : command.title().trim();
        JsonNode cleanContentJson = TipTapContent.sanitizeForStorage(command.contentJson());
        JournalPayload payload = new JournalPayload(1, title, cleanContentJson, plainText, command.location());
        SafetyDecision decision = screening.screen(title + "\n" + plainText);
        String status = decision.permitsOrdinaryAnalysis() ? "PENDING" : "BLOCKED_BY_SAFETY";
        JournalEntrySnapshot entry = new JournalEntrySnapshot(id, userId,
                cipher.encrypt(userId, id, mapper.writeValueAsString(payload)), occurredAt,
                occurredAt.atZone(ZoneId.of(timezone)).toLocalDate(), timezone, command.moodCode(),
                command.moodScore(), command.stressScore(), command.energyScore(), command.sleepMinutes(), false,
                "SAVED", decision.riskLevel().name(), status, 1, 0, now, now, null);
        store.create(entry);
        if (command.tagIds() != null && !command.tagIds().isEmpty()) {
            tagStore.replaceTags(userId, id, command.tagIds(), now);
        }
        if (assetStore != null) {
            assetStore.linkAssets(userId, id, assetIds, now);
        }
        safetyEvents.record(userId, id, decision, now);
        publish(id, 1, decision, now);
        idempotency.complete(userId, "journal-create", key, id, 201, now);
        return view(entry, tagStore.listTagsForEntry(userId, id));
    }

    @Transactional(readOnly = true)
    public JournalEntryView get(UUID userId, UUID entryId) {
        JournalEntrySnapshot entry = require(userId, entryId);
        return view(entry, tagStore.listTagsForEntry(userId, entryId));
    }

    @Transactional(readOnly = true)
    public List<JournalEntryView> exportAll(UUID userId) {
        List<JournalEntrySnapshot> entries = store.exportAll(userId, 10001);
        if (entries.size() > 10000) throw new IllegalStateException("Export row limit exceeded");
        List<UUID> entryIds = entries.stream().map(JournalEntrySnapshot::id).toList();
        Map<UUID, List<JournalTagView>> tagsByEntry = tagStore.listTagsForEntries(userId, entryIds);
        return entries.stream().map(e -> view(e, tagsByEntry.getOrDefault(e.id(), List.of()))).toList();
    }

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

        List<UUID> entryIds = page.stream().map(JournalEntrySnapshot::id).toList();
        Map<UUID, List<JournalTagView>> tagsByEntry = tagStore.listTagsForEntries(userId, entryIds);
        Map<UUID, JournalAsset> coverAssets = assetStore == null ? Map.of() : assetStore.findCoverAssetsForEntries(userId, entryIds);

        return new JournalPage(page.stream()
                .map(e -> {
                    String photoUrl = assetService == null ? null : assetService.resolveCoverUrl(coverAssets.get(e.id()));
                    return summary(e, tagsByEntry.getOrDefault(e.id(), List.of()), photoUrl);
                })
                .toList(), next);
    }

    /** Longest calendar range accepted: a month view with leading/trailing days fits comfortably. */
    public static final int DAILY_COUNTS_MAX_DAYS = 92;

    /** Saved entries per local day for the calendar; owner-scoped, no content is decrypted. */
    @Transactional(readOnly = true)
    public List<DailyEntryCountView> dailyCounts(UUID userId, LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to) || to.isAfter(from.plusDays(DAILY_COUNTS_MAX_DAYS - 1L)))
            throw new InvalidRequestException();
        return store.dailyCounts(userId, from, to);
    }

    @Transactional(readOnly = true)
    public JournalPage searchByTitle(UUID userId, String query, String cursor, int pageSize) {
        if (pageSize < 1 || pageSize > 50 || query == null || query.isBlank()) {
            throw new InvalidRequestException();
        }
        String normalizedQuery = VietnameseStringUtils.normalize(query);
        if (normalizedQuery.isBlank()) {
            return new JournalPage(List.of(), null);
        }

        Cursor currentCursor = decode(cursor);
        Instant cursorTime = currentCursor == null ? null : currentCursor.occurredAt();
        UUID cursorId = currentCursor == null ? null : currentCursor.id();

        final int chunkSize = 100;
        final int maxScanned = 1000;
        int totalScanned = 0;
        List<JournalEntrySnapshot> matched = new ArrayList<>();
        String nextCursor = null;

        while (matched.size() <= pageSize && totalScanned < maxScanned) {
            int fetchLimit = chunkSize + 1;
            List<JournalEntrySnapshot> chunk = store.list(userId, null, null, null, null, cursorTime, cursorId, fetchLimit);
            if (chunk.isEmpty()) {
                break;
            }

            int scanCount = Math.min(chunk.size(), chunkSize);
            for (int i = 0; i < scanCount; i++) {
                JournalEntrySnapshot entry = chunk.get(i);
                totalScanned++;
                String title = payload(entry).title();
                if (VietnameseStringUtils.matches(title, normalizedQuery)) {
                    matched.add(entry);
                    if (matched.size() > pageSize) {
                        break;
                    }
                }
            }

            if (matched.size() > pageSize) {
                nextCursor = encode(matched.get(pageSize - 1));
                break;
            }

            if (chunk.size() < chunkSize) {
                break;
            }

            JournalEntrySnapshot lastInChunk = chunk.get(chunk.size() - 1);
            cursorTime = lastInChunk.occurredAt();
            cursorId = lastInChunk.id();
        }

        List<JournalEntrySnapshot> pageEntries = matched.size() > pageSize
                ? matched.subList(0, pageSize)
                : matched;

        List<UUID> entryIds = pageEntries.stream().map(JournalEntrySnapshot::id).toList();
        Map<UUID, List<JournalTagView>> tagsByEntry = tagStore.listTagsForEntries(userId, entryIds);
        Map<UUID, JournalAsset> coverAssets = assetStore == null ? Map.of() : assetStore.findCoverAssetsForEntries(userId, entryIds);

        List<JournalSummaryView> views = pageEntries.stream()
                .map(e -> {
                    String photoUrl = assetService == null ? null : assetService.resolveCoverUrl(coverAssets.get(e.id()));
                    return summary(e, tagsByEntry.getOrDefault(e.id(), List.of()), photoUrl);
                })
                .toList();

        return new JournalPage(views, nextCursor);
    }

    @Transactional
    public JournalEntryView update(UUID userId, UUID entryId, UpdateJournalEntryCommand command, long expectedVersion) {
        JournalEntrySnapshot old = require(userId, entryId);
        if (old.rowVersion() != expectedVersion) throw new ConflictException("Nhật ký đã được cập nhật ở nơi khác.");
        JournalPayload previous = payload(old);
        String title = command.title() == null ? previous.title() : command.title().trim();
        JsonNode content = command.contentJson() == null ? previous.contentJson() : command.contentJson();

        List<UUID> assetIds = TipTapContent.extractAssetIds(content);
        if (assetIds.size() > 20) {
            throw new InvalidRequestException("Mỗi bài viết chỉ được đính kèm tối đa 20 hình ảnh.");
        }

        String plainText = TipTapContent.plainText(content);
        JsonNode cleanContent = TipTapContent.sanitizeForStorage(content);
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
                cipher.encrypt(userId, entryId, mapper.writeValueAsString(new JournalPayload(1, title, cleanContent, plainText, location))),
                occurredAt, occurredAt.atZone(ZoneId.of(timezone)).toLocalDate(), timezone, moodCode,
                mood, stress, energy, sleep, old.favorite(), old.entryStatus(), decision.riskLevel().name(),
                decision.permitsOrdinaryAnalysis() ? "ANALYSIS_OUTDATED" : "BLOCKED_BY_SAFETY",
                old.contentVersion() + 1, old.rowVersion() + 1, old.createdAt(), now, null);
        if (!store.update(next, expectedVersion)) throw new ConflictException("Nhật ký đã được cập nhật ở nơi khác.");
        if (command.tagIds() != null) {
            tagStore.replaceTags(userId, entryId, command.tagIds(), now);
        }
        if (assetStore != null) {
            assetStore.linkAssets(userId, entryId, assetIds, now);
        }
        safetyEvents.record(userId, entryId, decision, now);
        outbox.publish("JOURNAL_ENTRY", entryId, "JournalEntryChanged", next.contentVersion(), now);
        publish(entryId, next.contentVersion(), decision, now);
        return view(next, tagStore.listTagsForEntry(userId, entryId));
    }

    @Transactional
    public void delete(UUID userId, UUID entryId, long expectedVersion) {
        JournalEntrySnapshot old = require(userId, entryId);
        if (!store.softDelete(userId, entryId, expectedVersion, clock.instant())) {
            throw new ConflictException("Nhật ký đã được cập nhật ở nơi khác.");
        }
        if (assetStore != null) {
            assetStore.softDeleteAssetsForEntry(userId, entryId, clock.instant());
        }
        outbox.publish("JOURNAL_ENTRY", entryId, "JournalEntryDeleted", old.contentVersion(), clock.instant());
    }

    @Transactional
    public JournalEntryView favorite(UUID userId, UUID entryId, boolean favorite) {
        if (!store.setFavorite(userId, entryId, favorite, clock.instant())) throw notFound();
        return get(userId, entryId);
    }

    @Override @Transactional(readOnly = true)
    public Input load(UUID userId, UUID entryId, int contentVersion) {
        return store.find(userId, entryId).filter(e -> e.contentVersion() == contentVersion
                        && ("PENDING".equals(e.analysisStatus()) || "ANALYZING".equals(e.analysisStatus())
                        || "ANALYSIS_OUTDATED".equals(e.analysisStatus())))
                .map(e -> {
                    JournalPayload data = payload(e);
                    return new Input(userId, entryId, contentVersion, data.title(), data.plainText());
                }).orElse(null);
    }

    @Override @Transactional
    public boolean start(UUID userId, UUID entryId, int contentVersion, Instant now) {
        return store.transitionAnalysis(userId, entryId, contentVersion,
                List.of("PENDING", "ANALYSIS_OUTDATED", "ANALYSIS_FAILED", "ANALYZING"),
                "ANALYZING", null, now);
    }

    @Override @Transactional
    public boolean activate(UUID userId, UUID entryId, int contentVersion, UUID analysisId, Instant now) {
        return store.transitionAnalysis(userId, entryId, contentVersion,
                List.of("ANALYZING"), "ANALYZED", analysisId, now);
    }

    @Override @Transactional
    public void fail(UUID userId, UUID entryId, int contentVersion, Instant now) {
        store.transitionAnalysis(userId, entryId, contentVersion,
                List.of("ANALYZING"), "ANALYSIS_FAILED", null, now);
    }

    @Override @Transactional
    public void block(UUID userId, UUID entryId, int contentVersion, Instant now) {
        store.transitionAnalysis(userId, entryId, contentVersion,
                List.of("PENDING", "ANALYSIS_OUTDATED", "ANALYZING", "ANALYSIS_FAILED"),
                "BLOCKED_BY_SAFETY", null, now);
    }

    @Override @Transactional
    public void cancel(UUID userId, UUID entryId, int contentVersion, Instant now) {
        store.transitionAnalysis(userId, entryId, contentVersion,
                List.of("PENDING", "ANALYSIS_OUTDATED", "ANALYZING", "ANALYSIS_FAILED"),
                "NOT_REQUESTED", null, now);
    }

    @Override @Transactional
    public boolean retry(UUID userId, UUID entryId, int contentVersion, Instant now) {
        return store.transitionAnalysis(userId, entryId, contentVersion,
                List.of("ANALYSIS_FAILED", "NOT_REQUESTED"), "PENDING", null, now);
    }

    @Transactional
    public boolean rescreen(UUID userId, UUID entryId, int contentVersion) {
        JournalEntrySnapshot entry = store.find(userId, entryId).orElse(null);
        if (entry == null || entry.contentVersion() != contentVersion
                || !"BLOCKED_BY_SAFETY".equals(entry.analysisStatus())) return true;
        JournalPayload data = payload(entry);
        SafetyDecision decision = screening.screen(data.title() + "\n" + data.plainText());
        if ("FAIL_SAFE".equals(decision.decision())) return false;
        Instant now = clock.instant();
        if (!store.applyRescreen(userId, entryId, contentVersion, decision.riskLevel().name(),
                decision.permitsOrdinaryAnalysis() ? "PENDING" : "BLOCKED_BY_SAFETY", now)) return true;
        safetyEvents.record(userId, entryId, decision, now);
        if (decision.permitsOrdinaryAnalysis())
            outbox.publish("JOURNAL_ENTRY", entryId, "JournalEntrySubmitted", contentVersion, now);
        return true;
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

    private JournalEntryView view(JournalEntrySnapshot entry, List<JournalTagView> tags) {
        JournalPayload data = payload(entry);
        JsonNode enrichedContent = assetService == null ? data.contentJson() : assetService.enrichContentWithSignedUrls(entry.userId(), data.contentJson());
        String photoUrl = null;
        if (assetStore != null && assetService != null) {
            Map<UUID, JournalAsset> covers = assetStore.findCoverAssetsForEntries(entry.userId(), List.of(entry.id()));
            JournalAsset cover = covers.get(entry.id());
            photoUrl = assetService.resolveCoverUrl(cover);
        }
        return new JournalEntryView(entry.id(), data.title(), enrichedContent, data.location(),
                entry.occurredAt(), entry.localDate(), entry.timezone(), entry.moodCode(), entry.moodScore(),
                entry.stressScore(), entry.energyScore(), entry.sleepMinutes(), entry.favorite(),
                entry.entryStatus(), entry.riskLevel(), entry.analysisStatus(), entry.contentVersion(),
                entry.rowVersion(), entry.createdAt(), entry.updatedAt(), tags == null ? List.of() : tags, photoUrl);
    }

    private JournalEntryView view(JournalEntrySnapshot entry) {
        return view(entry, List.of());
    }

    private JournalSummaryView summary(JournalEntrySnapshot entry, List<JournalTagView> tags, String photoUrl) {
        return new JournalSummaryView(entry.id(), payload(entry).title(), entry.occurredAt(), entry.localDate(),
                entry.moodCode(), entry.favorite(), entry.riskLevel(), entry.analysisStatus(), entry.rowVersion(),
                tags == null ? List.of() : tags, photoUrl);
    }

    private JournalSummaryView summary(JournalEntrySnapshot entry, List<JournalTagView> tags) {
        return summary(entry, tags, null);
    }

    private JournalSummaryView summary(JournalEntrySnapshot entry) {
        return summary(entry, List.of(), null);
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
