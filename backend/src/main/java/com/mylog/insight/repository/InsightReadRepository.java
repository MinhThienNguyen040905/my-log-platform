package com.mylog.insight.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

@Repository
public class InsightReadRepository {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public InsightReadRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public List<InsightRow> findVisible(UUID userId) {
        return jdbc.query("""
                SELECT id, type, title, description, confidence, status, period_start, period_end,
                       created_at, updated_at
                FROM insights WHERE user_id = ? AND status IN ('ACTIVE', 'FADING')
                ORDER BY period_end DESC, confidence DESC, updated_at DESC
                """, (rs, row) -> insightRow(rs), userId);
    }

    public Optional<InsightRow> findOwned(UUID userId, UUID insightId) {
        return jdbc.query("""
                SELECT id, type, title, description, confidence, status, period_start, period_end,
                       created_at, updated_at
                FROM insights WHERE id = ? AND user_id = ?
                """, (rs, row) -> insightRow(rs), insightId, userId).stream().findFirst();
    }

    public List<EvidenceRow> findEvidence(UUID insightId) {
        return jdbc.query("""
                SELECT id, evidence_type, sample_size, matching_count, metric, numeric_value,
                       unit, evidence_json::text AS evidence_json, calculation_version
                FROM insight_evidence WHERE insight_id = ? ORDER BY created_at
                """, (rs, row) -> new EvidenceRow(
                        rs.getObject("id", UUID.class), rs.getString("evidence_type"), rs.getInt("sample_size"),
                        (Integer) rs.getObject("matching_count"), rs.getString("metric"),
                        rs.getBigDecimal("numeric_value"), rs.getString("unit"), rs.getString("evidence_json"),
                        rs.getString("calculation_version")), insightId);
    }

    public List<ActionRow> findActions(UUID insightId) {
        return jdbc.query("""
                SELECT id, description, status, created_at, responded_at
                FROM suggested_actions WHERE insight_id = ?
                ORDER BY created_at DESC LIMIT 1
                """, (rs, row) -> new ActionRow(
                        rs.getObject("id", UUID.class), rs.getString("description"), rs.getString("status"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("responded_at") == null ? null : rs.getTimestamp("responded_at").toInstant()),
                insightId);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> parseDetails(String json) {
        return objectMapper.readValue(json, Map.class);
    }

    private InsightRow insightRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new InsightRow(
                rs.getObject("id", UUID.class), rs.getString("type"), rs.getString("title"),
                rs.getString("description"), rs.getString("confidence"), rs.getString("status"),
                rs.getObject("period_start", LocalDate.class), rs.getObject("period_end", LocalDate.class),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    public record InsightRow(
            UUID id, String type, String title, String description, String confidence, String status,
            LocalDate periodStart, LocalDate periodEnd, Instant createdAt, Instant updatedAt) {}

    public record EvidenceRow(
            UUID id, String type, int sampleSize, Integer matchingCount, String metric,
            BigDecimal numericValue, String unit, String detailsJson, String calculationVersion) {}

    public record ActionRow(UUID id, String description, String status, Instant createdAt, Instant respondedAt) {}
}
