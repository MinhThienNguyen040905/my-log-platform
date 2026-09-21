package com.mylog.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("integration-test")
class InfrastructureIT extends AbstractIntegrationTest {

    private static final List<String> EXPECTED_TABLES = List.of(
            "ai_usage_records",
            "analysis_jobs",
            "idempotency_records",
            "journal_analyses",
            "journal_corrections",
            "journal_emotions",
            "journal_entries",
            "journal_topics",
            "outbox_events",
            "processed_messages",
            "reflection_questions",
            "reflection_responses",
            "refresh_tokens",
            "safety_events",
            "topics",
            "user_consents",
            "user_preferences",
            "users");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ConnectionFactory rabbitConnectionFactory;

    @Test
    void infrastructureIsAvailableAndSchemaIsMigrated() {
        Integer migrationCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class);
        assertThat(migrationCount).isEqualTo(6);

        List<String> tables = jdbcTemplate.queryForList(
                """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name <> 'flyway_schema_history'
                ORDER BY table_name
                """,
                String.class);
        assertThat(tables).containsExactlyElementsOf(EXPECTED_TABLES);

        List<String> rlsTables = jdbcTemplate.queryForList(
                """
                SELECT c.relname
                FROM pg_class c
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = 'public'
                  AND c.relrowsecurity
                ORDER BY c.relname
                """,
                String.class);
        assertThat(rlsTables).containsExactlyElementsOf(EXPECTED_TABLES);

        assertThat(redisTemplate.getConnectionFactory()).isNotNull();
        try (var connection = redisTemplate.getConnectionFactory().getConnection()) {
            assertThat(connection.ping()).isEqualTo("PONG");
        }

        try (var connection = rabbitConnectionFactory.createConnection()) {
            assertThat(connection.isOpen()).isTrue();
        }
    }
}
