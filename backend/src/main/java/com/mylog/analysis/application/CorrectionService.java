package com.mylog.analysis.application;

import com.mylog.analysis.api.CorrectionRequest;
import com.mylog.analysis.api.CorrectionRequest.EmotionCorrection;
import com.mylog.analysis.api.CorrectionRequest.Operation;
import com.mylog.analysis.api.CorrectionRequest.TopicCorrection;
import com.mylog.shared.api.ApiErrorCodes;
import com.mylog.shared.exception.BadRequestException;
import com.mylog.shared.exception.ConflictException;
import com.mylog.shared.messaging.MessagingTopology;
import com.mylog.shared.outbox.OutboxWriter;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class CorrectionService {

    private static final Set<String> EMOTIONS = Set.of(
            "JOY", "SADNESS", "ANGER", "FEAR", "ANXIETY", "CALM", "HOPE",
            "GRATITUDE", "LONELINESS", "FRUSTRATION", "EXCITEMENT");

    private final JdbcTemplate jdbc;
    private final AnalysisQueryService query;
    private final OutboxWriter outbox;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public CorrectionService(
            JdbcTemplate jdbc, AnalysisQueryService query, OutboxWriter outbox,
            ObjectMapper objectMapper, Clock clock) {
        this.jdbc = jdbc;
        this.query = query;
        this.outbox = outbox;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public void correct(UUID userId, UUID journalId, CorrectionRequest request) {
        AnalysisQueryService.JournalState journal = query.ownedJournal(userId, journalId);
        UUID analysisId = currentAnalysis(journalId, journal.version());
        List<EmotionCorrection> emotions = request.emotions() == null ? List.of() : request.emotions();
        List<TopicCorrection> topics = request.topics() == null ? List.of() : request.topics();
        if (emotions.isEmpty() && topics.isEmpty()) {
            throw new BadRequestException(ApiErrorCodes.INVALID_REQUEST, "At least one correction is required");
        }
        Instant now = clock.instant();
        emotions.forEach(item -> correctEmotion(userId, journalId, analysisId, item, now));
        topics.forEach(item -> correctTopic(userId, journalId, analysisId, item, now));
        outbox.append("JOURNAL", journalId, MessagingTopology.JOURNAL_CORRECTED, 1,
                Map.of("journalId", journalId.toString(), "userId", userId.toString(), "journalVersion", journal.version()));
    }

    private void correctEmotion(
            UUID userId, UUID journalId, UUID analysisId, EmotionCorrection item, Instant now) {
        String type = item.type().trim().toUpperCase(Locale.ROOT);
        if (!EMOTIONS.contains(type) || (item.operation() != Operation.REMOVE && item.score() == null)) {
            throw new BadRequestException(ApiErrorCodes.INVALID_REQUEST, "Emotion correction is invalid");
        }
        List<BigDecimal> originals = jdbc.queryForList(
                "SELECT original_score FROM journal_emotions WHERE analysis_id = ? AND emotion_type = ?",
                BigDecimal.class, analysisId, type);
        BigDecimal original = originals.isEmpty() ? null : originals.getFirst();
        BigDecimal effective = item.operation() == Operation.REMOVE ? BigDecimal.ZERO : item.score();
        if (originals.isEmpty()) {
            jdbc.update("""
                    INSERT INTO journal_emotions (
                        id, analysis_id, emotion_type, original_score, corrected_score,
                        corrected_by_user, corrected_at, created_at)
                    VALUES (?, ?, ?, 0, ?, TRUE, ?, ?)
                    """, UUID.randomUUID(), analysisId, type, effective, Timestamp.from(now), Timestamp.from(now));
        } else {
            jdbc.update("""
                    UPDATE journal_emotions SET corrected_score = ?, corrected_by_user = TRUE, corrected_at = ?
                    WHERE analysis_id = ? AND emotion_type = ?
                    """, effective, Timestamp.from(now), analysisId, type);
        }
        audit(userId, journalId, analysisId, "EMOTION", type, item.operation(),
                original == null ? null : Map.of("score", original), Map.of("score", effective), now);
    }

    private void correctTopic(
            UUID userId, UUID journalId, UUID analysisId, TopicCorrection item, Instant now) {
        String displayName = item.name().trim();
        String normalized = normalizeTopic(displayName);
        UUID topicId = jdbc.queryForObject("""
                INSERT INTO topics (id, normalized_name, display_name, created_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (normalized_name) DO UPDATE SET display_name = topics.display_name
                RETURNING id
                """, UUID.class, UUID.randomUUID(), normalized, displayName, Timestamp.from(now));
        List<Map<String, Object>> existing = jdbc.queryForList("""
                SELECT source, confidence, is_active FROM journal_topics
                WHERE journal_entry_id = ? AND topic_id = ?
                """, journalId, topicId);
        Map<String, Object> original = existing.isEmpty() ? null : existing.getFirst();
        if (item.operation() == Operation.REMOVE) {
            if (!existing.isEmpty()) {
                jdbc.update("UPDATE journal_topics SET is_active = FALSE, updated_at = ? WHERE journal_entry_id = ? AND topic_id = ?",
                        Timestamp.from(now), journalId, topicId);
            }
        } else {
            BigDecimal confidence = item.confidence() == null ? BigDecimal.ONE : item.confidence();
            jdbc.update("""
                    INSERT INTO journal_topics (
                        journal_entry_id, topic_id, source, confidence, is_active, created_at, updated_at)
                    VALUES (?, ?, 'USER', ?, TRUE, ?, ?)
                    ON CONFLICT (journal_entry_id, topic_id) DO UPDATE
                    SET confidence = EXCLUDED.confidence, is_active = TRUE, updated_at = EXCLUDED.updated_at
                    """, journalId, topicId, confidence, Timestamp.from(now), Timestamp.from(now));
        }
        Map<String, Object> corrected = new LinkedHashMap<>();
        corrected.put("active", item.operation() != Operation.REMOVE);
        corrected.put("confidence", item.confidence());
        audit(userId, journalId, analysisId, "TOPIC", normalized, item.operation(), original, corrected, now);
    }

    private void audit(
            UUID userId, UUID journalId, UUID analysisId, String fieldType, String fieldKey,
            Operation operation, Object original, Object corrected, Instant now) {
        jdbc.update("""
                INSERT INTO journal_corrections (
                    id, user_id, journal_entry_id, analysis_id, field_type, field_key,
                    operation, original_value, corrected_value, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?)
                """, UUID.randomUUID(), userId, journalId, analysisId, fieldType, fieldKey,
                operation.name(), original == null ? null : objectMapper.writeValueAsString(original),
                corrected == null ? null : objectMapper.writeValueAsString(corrected), Timestamp.from(now));
    }

    private UUID currentAnalysis(UUID journalId, long version) {
        List<UUID> ids = jdbc.queryForList("""
                SELECT id FROM journal_analyses
                WHERE journal_entry_id = ? AND journal_version = ? AND is_current = TRUE
                """, UUID.class, journalId, version);
        if (ids.isEmpty()) {
            throw new ConflictException(ApiErrorCodes.ANALYSIS_NOT_READY, "Current analysis is not completed");
        }
        return ids.getFirst();
    }

    private String normalizeTopic(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
