package com.mylog.integration.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.mylog.feedback.service.FeedbackService;
import com.mylog.insight.service.InsightService;
import com.mylog.statistics.service.StatisticsService;
import com.mylog.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("integration-test")
class AnalyticsFeedbackIT extends AbstractIntegrationTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private StatisticsService statistics;
    @Autowired private InsightService insights;
    @Autowired private FeedbackService feedback;

    @BeforeEach
    void cleanState() {
        jdbc.execute("TRUNCATE TABLE processed_messages, outbox_events");
        jdbc.execute("TRUNCATE TABLE users CASCADE");
    }

    @Test
    void timezoneBoundaryAndEffectiveCorrectionAreUsedWithoutTurningNullIntoZero() {
        UUID userId = user("UTC");
        Instant boundary = LocalDate.now(ZoneId.of("UTC"))
                .atStartOfDay(ZoneId.of("UTC")).toInstant().plus(30, ChronoUnit.MINUTES);
        UUID journalId = journal(userId, boundary, 8, null, null);
        UUID analysisId = analysis(journalId);
        jdbc.update("""
                INSERT INTO journal_emotions (
                    id, analysis_id, emotion_type, original_score, corrected_score,
                    corrected_by_user, corrected_at, created_at)
                VALUES (?, ?, 'JOY', 0.2000, 0.9000, TRUE, ?, ?)
                """, UUID.randomUUID(), analysisId, Timestamp.from(boundary), Timestamp.from(boundary));

        LocalDate newYorkDate = boundary.atZone(ZoneId.of("America/New_York")).toLocalDate();
        var mood = statistics.mood(userId, newYorkDate, newYorkDate, "America/New_York");
        var emotions = statistics.emotions(userId, newYorkDate, newYorkDate, "America/New_York");

        assertThat(mood.days()).hasSize(1);
        assertThat(mood.days().getFirst().stressAverage()).isNull();
        assertThat(mood.days().getFirst().energyAverage()).isNull();
        assertThat(emotions.emotions()).singleElement()
                .extracting(item -> item.averageScore())
                .isEqualTo(new BigDecimal("0.9000"));
    }

    @Test
    void insightRequiresThreeDaysAndFeedbackPutIsIdempotent() {
        UUID userId = user("UTC");
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        UUID topicId = UUID.randomUUID();
        jdbc.update("INSERT INTO topics (id, normalized_name, display_name, created_at) VALUES (?, 'work', 'Work', ?)",
                topicId, Timestamp.from(Instant.now()));

        for (int day = 0; day < 2; day++) {
            UUID high = journal(userId, today.minusDays(day).atTime(12, 0).toInstant(java.time.ZoneOffset.UTC), 9, 5, 5);
            journal(userId, today.minusDays(day).atTime(18, 0).toInstant(java.time.ZoneOffset.UTC), 1, 5, 5);
            linkTopic(high, topicId);
        }
        insights.refresh(userId);
        assertThat(insights.list(userId)).isEmpty();

        UUID high = journal(userId, today.minusDays(2).atTime(12, 0).toInstant(java.time.ZoneOffset.UTC), 9, 5, 5);
        journal(userId, today.minusDays(2).atTime(18, 0).toInstant(java.time.ZoneOffset.UTC), 1, 5, 5);
        linkTopic(high, topicId);
        insights.refresh(userId);

        var insight = insights.list(userId).getFirst();
        assertThat(insight.confidence()).isEqualTo("WEAK");
        assertThat(insight.evidence()).singleElement().satisfies(evidence -> {
            assertThat(evidence.calculationVersion()).isEqualTo("topic_mood_v1");
            assertThat(evidence.details()).containsEntry("interpretation", "ASSOCIATION_NOT_CAUSATION");
        });
        assertThat(insight.suggestedActions()).hasSize(1);

        var first = feedback.put(userId, "insight", insight.id(), "helpful");
        var second = feedback.put(userId, "INSIGHT", insight.id(), "NOT_HELPFUL");
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.value()).isEqualTo("NOT_HELPFUL");
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM feedback WHERE user_id = ? AND target_id = ?",
                Integer.class, userId, insight.id())).isEqualTo(1);
    }

    private UUID user(String timezone) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.update("""
                INSERT INTO users (
                    id, email, email_normalized, password_hash, display_name,
                    plan, status, created_at, updated_at, version)
                VALUES (?, ?, ?, 'hash', 'Analytics User', 'FREE', 'ACTIVE', ?, ?, 0)
                """, id, id + "@example.test", id + "@example.test", Timestamp.from(now), Timestamp.from(now));
        jdbc.update("""
                INSERT INTO user_preferences (
                    user_id, timezone, language, is_onboarded, journaling_goals, created_at, updated_at)
                VALUES (?, ?, 'vi', TRUE, '[]'::jsonb, ?, ?)
                """, id, timezone, Timestamp.from(now), Timestamp.from(now));
        return id;
    }

    private UUID journal(
            UUID userId, Instant occurredAt, int mood, Integer stress, Integer energy) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.update("""
                INSERT INTO journal_entries (
                    id, user_id, content_text, content_format, mood_score, stress_score, energy_score,
                    status, journal_version, occurred_at, entry_date, timezone_at_entry,
                    is_favorite, created_at, updated_at, version)
                VALUES (?, ?, 'analytics fixture', 'PLAIN_TEXT', ?, ?, ?, 'SAVED', 1, ?, ?, 'UTC', FALSE, ?, ?, 0)
                """, id, userId, mood, stress, energy, Timestamp.from(occurredAt),
                occurredAt.atZone(ZoneId.of("UTC")).toLocalDate(), Timestamp.from(now), Timestamp.from(now));
        return id;
    }

    private UUID analysis(UUID journalId) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.update("""
                INSERT INTO journal_analyses (
                    id, journal_entry_id, journal_version, sentiment, risk_level, provider, model,
                    prompt_version, schema_version, is_current, analyzed_at, created_at)
                VALUES (?, ?, 1, 'POSITIVE', 'NORMAL', 'mock', 'mock', 'v1', '1.0', TRUE, ?, ?)
                """, id, journalId, Timestamp.from(now), Timestamp.from(now));
        return id;
    }

    private void linkTopic(UUID journalId, UUID topicId) {
        Instant now = Instant.now();
        jdbc.update("""
                INSERT INTO journal_topics (
                    journal_entry_id, topic_id, source, confidence, is_active, created_at, updated_at)
                VALUES (?, ?, 'AI', 0.9000, TRUE, ?, ?)
                """, journalId, topicId, Timestamp.from(now), Timestamp.from(now));
    }
}
