package com.mylog.analysis.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

@Repository
public class CorrectionRepository {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public CorrectionRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public Optional<UUID> findCurrentAnalysisId(UUID journalId, long version) {
        return jdbc.queryForList("""
                SELECT id FROM journal_analyses
                WHERE journal_entry_id = ? AND journal_version = ? AND is_current = TRUE
                """, UUID.class, journalId, version).stream().findFirst();
    }

    public Optional<BigDecimal> findEmotionOriginalScore(UUID analysisId, String type) {
        return jdbc.queryForList(
                "SELECT original_score FROM journal_emotions WHERE analysis_id = ? AND emotion_type = ?",
                BigDecimal.class, analysisId, type).stream().findFirst();
    }

    public void insertEmotion(UUID analysisId, String type, BigDecimal score, Instant now) {
        jdbc.update("""
                INSERT INTO journal_emotions (
                    id, analysis_id, emotion_type, original_score, corrected_score,
                    corrected_by_user, corrected_at, created_at)
                VALUES (?, ?, ?, 0, ?, TRUE, ?, ?)
                """, UUID.randomUUID(), analysisId, type, score, Timestamp.from(now), Timestamp.from(now));
    }

    public void updateEmotion(UUID analysisId, String type, BigDecimal score, Instant now) {
        jdbc.update("""
                UPDATE journal_emotions SET corrected_score = ?, corrected_by_user = TRUE, corrected_at = ?
                WHERE analysis_id = ? AND emotion_type = ?
                """, score, Timestamp.from(now), analysisId, type);
    }

    public UUID upsertTopic(String normalizedName, String displayName, Instant now) {
        return jdbc.queryForObject("""
                INSERT INTO topics (id, normalized_name, display_name, created_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (normalized_name) DO UPDATE SET display_name = topics.display_name
                RETURNING id
                """, UUID.class, UUID.randomUUID(), normalizedName, displayName, Timestamp.from(now));
    }

    public Optional<Map<String, Object>> findJournalTopic(UUID journalId, UUID topicId) {
        return jdbc.queryForList("""
                SELECT source, confidence, is_active FROM journal_topics
                WHERE journal_entry_id = ? AND topic_id = ?
                """, journalId, topicId).stream().findFirst();
    }

    public void deactivateTopic(UUID journalId, UUID topicId, Instant now) {
        jdbc.update("""
                UPDATE journal_topics SET is_active = FALSE, updated_at = ?
                WHERE journal_entry_id = ? AND topic_id = ?
                """, Timestamp.from(now), journalId, topicId);
    }

    public void upsertUserTopic(UUID journalId, UUID topicId, BigDecimal confidence, Instant now) {
        jdbc.update("""
                INSERT INTO journal_topics (
                    journal_entry_id, topic_id, source, confidence, is_active, created_at, updated_at)
                VALUES (?, ?, 'USER', ?, TRUE, ?, ?)
                ON CONFLICT (journal_entry_id, topic_id) DO UPDATE
                SET confidence = EXCLUDED.confidence, is_active = TRUE, updated_at = EXCLUDED.updated_at
                """, journalId, topicId, confidence, Timestamp.from(now), Timestamp.from(now));
    }

    public void insertAudit(
            UUID userId, UUID journalId, UUID analysisId, String fieldType, String fieldKey,
            String operation, Object original, Object corrected, Instant now) {
        jdbc.update("""
                INSERT INTO journal_corrections (
                    id, user_id, journal_entry_id, analysis_id, field_type, field_key,
                    operation, original_value, corrected_value, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?)
                """, UUID.randomUUID(), userId, journalId, analysisId, fieldType, fieldKey, operation,
                original == null ? null : objectMapper.writeValueAsString(original),
                corrected == null ? null : objectMapper.writeValueAsString(corrected), Timestamp.from(now));
    }
}
