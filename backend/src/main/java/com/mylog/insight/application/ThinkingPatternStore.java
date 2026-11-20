package com.mylog.insight.application;

import com.mylog.insight.domain.ThinkingPatternRules;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Persistence port for thinking-pattern cards (pattern_recognitions + evidence) and the signals they read. */
public interface ThinkingPatternStore {
    /** A stored card as the API shows it; quotes already decrypted for the owner. */
    record Card(UUID id, String code, LocalDate periodStart, LocalDate periodEnd, String status, String feedback,
                String feedbackReason, Instant feedbackAt, int entryCount, int weekCount, String narrative,
                String generatedBy, UUID knowledgeVersionId, UUID planGoalId, Instant planStartedAt,
                Instant createdAt, Instant updatedAt, List<Quote> quotes) {}

    record Quote(String text, LocalDate date) {}

    /** Values written for a created or refreshed card. */
    record Write(String code, LocalDate periodStart, LocalDate periodEnd, String timezone,
                 ThinkingPatternRules.Stats stats, String narrative, UUID knowledgeVersionId) {}

    /** Serialises refreshes of one user. */
    void lock(UUID userId);

    /** Signals of current, readable (saved, not deleted, risk NORMAL/LOW) entries in the window. */
    List<ThinkingPatternRules.SignalPoint> signals(UUID userId, LocalDate from, LocalDate to, String taxonomyVersion);

    List<ThinkingPatternRules.Existing> existing(UUID userId, String taxonomyVersion);

    /** The approved, effective knowledge version for {@code topicCode} and {@code locale}, if any. No provider call. */
    Optional<UUID> approvedKnowledgeVersion(String topicCode, String locale, Instant now);

    UUID create(UUID userId, String taxonomyVersion, Write write, Instant now);

    void update(UUID userId, UUID cardId, Write write, Instant now);

    void supersede(UUID userId, List<UUID> cardIds, Instant now);

    List<Card> active(UUID userId);

    Optional<Card> find(UUID userId, UUID cardId);

    /** NOT_REALLY also retires the card; CONFIRMED keeps it. False when the card is not the user's live card. */
    boolean feedback(UUID userId, UUID cardId, String feedback, String reason, Instant now);

    boolean hide(UUID userId, UUID cardId, Instant now);

    /** Sets the plan goal once; false when the card already has a plan or is not the user's live card. */
    boolean attachPlan(UUID userId, UUID cardId, UUID goalId, Instant now);

    /** Every card of the user without quotes or narrative, for the data export. */
    List<Map<String, Object>> exportAll(UUID userId);
}
