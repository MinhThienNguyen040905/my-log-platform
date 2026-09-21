package com.mylog.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("supabase")
@EnabledIfEnvironmentVariable(named = "RUN_SUPABASE_IT", matches = "true")
class SupabaseSchemaIT {

    private static final List<String> APPLICATION_TABLES = List.of(
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

    @Test
    void schemaIsMigratedAndProtectedFromSupabaseDataApiRoles() {
        String version = jdbcTemplate.queryForObject(
                """
                SELECT version
                FROM flyway_schema_history
                WHERE success
                ORDER BY installed_rank DESC
                LIMIT 1
                """,
                String.class);
        assertThat(version).isEqualTo("006");

        List<String> tables = jdbcTemplate.queryForList(
                """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                """,
                String.class);
        assertThat(tables).containsAll(APPLICATION_TABLES);

        List<String> rlsTables = jdbcTemplate.queryForList(
                """
                SELECT c.relname
                FROM pg_class c
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = 'public'
                  AND c.relrowsecurity
                """,
                String.class);
        assertThat(rlsTables).containsAll(APPLICATION_TABLES);

        List<String> indexes = jdbcTemplate.queryForList(
                """
                SELECT indexname
                FROM pg_indexes
                WHERE schemaname = 'public'
                """,
                String.class);
        assertThat(indexes).contains(
                "idx_users_status",
                "idx_user_consents_user_active",
                "idx_refresh_tokens_user_active");

        List<String> refreshTokenColumns = jdbcTemplate.queryForList(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'refresh_tokens'
                """,
                String.class);
        assertThat(refreshTokenColumns).contains("created_by_ip_hash", "user_agent_hash");

        assertThat(hasSchemaUsage("anon")).isFalse();
        assertThat(hasSchemaUsage("authenticated")).isFalse();
        assertThat(hasTableSelect("anon", "users")).isFalse();
        assertThat(hasTableSelect("authenticated", "users")).isFalse();
        assertThat(hasTableSelect("anon", "user_consents")).isFalse();
        assertThat(hasTableSelect("authenticated", "user_consents")).isFalse();
    }

    private Boolean hasSchemaUsage(String role) {
        return jdbcTemplate.queryForObject(
                "SELECT has_schema_privilege(?, 'public', 'USAGE')",
                Boolean.class,
                role);
    }

    private Boolean hasTableSelect(String role, String table) {
        return jdbcTemplate.queryForObject(
                "SELECT has_table_privilege(?, 'public.' || ?, 'SELECT')",
                Boolean.class,
                role,
                table);
    }
}
