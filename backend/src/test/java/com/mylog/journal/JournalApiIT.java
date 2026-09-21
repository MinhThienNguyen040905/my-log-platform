package com.mylog.journal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mylog.support.AbstractIntegrationTest;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "spring.main.web-application-type=servlet")
@AutoConfigureMockMvc
@ActiveProfiles({"api", "integration-test"})
class JournalApiIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "StrongPass123";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private StringRedisTemplate redisTemplate;

    @BeforeEach
    void cleanState() {
        jdbcTemplate.execute("TRUNCATE TABLE processed_messages, outbox_events");
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
    }

    @Test
    void crudSupportsMultipleEntriesPerLocalDayAndSoftDelete() throws Exception {
        Session session = register("journal@example.com");
        Created first = create(session, journalBody("First entry", 4), null);
        Created second = create(session, journalBody("Second entry", 8), null);

        assertThat(first.body().get("entryDate").asText()).isEqualTo("2026-09-21");
        assertThat(second.body().get("entryDate").asText()).isEqualTo("2026-09-21");
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM journal_entries WHERE entry_date = DATE '2026-09-21'",
                        Integer.class))
                .isEqualTo(2);

        mockMvc.perform(get("/api/v1/journals/{id}", first.id())
                        .header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.contentText").value("First entry"));

        mockMvc.perform(delete("/api/v1/journals/{id}", first.id())
                        .header("Authorization", bearer(session)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/journals/{id}", first.id())
                        .header("Authorization", bearer(session)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("JOURNAL_NOT_FOUND"));

        mockMvc.perform(get("/api/v1/journals")
                        .header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(second.id().toString()));

        assertThat(jdbcTemplate.queryForObject(
                        "SELECT deleted_at IS NOT NULL FROM journal_entries WHERE id = ?",
                        Boolean.class,
                        first.id()))
                .isTrue();
    }

    @Test
    void validatesContentScoresTimezoneAndDateRange() throws Exception {
        Session session = register("validation@example.com");
        Map<String, Object> invalid = journalBody("   ", 11);
        invalid.put("timezoneAtEntry", "Not/A-Timezone");

        mockMvc.perform(post("/api/v1/journals")
                        .header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(get("/api/v1/journals")
                        .header("Authorization", bearer(session))
                        .queryParam("from", "2026-09-22")
                        .queryParam("to", "2026-09-20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(get("/api/v1/journals")
                        .header("Authorization", bearer(session))
                        .queryParam("cursor", "not-a-valid-cursor"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURSOR"));
    }

    @Test
    void cursorPaginationRemainsStableWhenANewerEntryIsInserted() throws Exception {
        Session session = register("pagination@example.com");
        Created oldest = create(session, journalBody("Oldest", 4), null);
        create(session, journalBody("Middle", 5), null);
        create(session, journalBody("Newest", 6), null);

        MvcResult firstPageResult = mockMvc.perform(get("/api/v1/journals")
                        .header("Authorization", bearer(session))
                        .queryParam("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.hasMore").value(true))
                .andReturn();
        JsonNode firstPage = body(firstPageResult);
        String cursor = firstPage.get("nextCursor").asText();
        List<String> firstIds = new ArrayList<>();
        firstPage.get("items").forEach(item -> firstIds.add(item.get("id").asText()));

        Created insertedAfterPageOne = create(session, journalBody("Inserted later", 7), null);

        MvcResult secondPageResult = mockMvc.perform(get("/api/v1/journals")
                        .header("Authorization", bearer(session))
                        .queryParam("limit", "2")
                        .queryParam("cursor", cursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.hasMore").value(false))
                .andReturn();
        JsonNode secondPage = body(secondPageResult);
        assertThat(secondPage.get("items").get(0).get("id").asText())
                .isEqualTo(oldest.id().toString());
        assertThat(firstIds).doesNotContain(insertedAfterPageOne.id().toString());
    }

    @Test
    void optimisticVersionAndJournalVersionFollowDifferentRules() throws Exception {
        Session session = register("version@example.com");
        Created created = create(session, journalBody("Original", 5), null);
        Map<String, Object> favoriteUpdate = new LinkedHashMap<>();
        favoriteUpdate.put("version", 0);
        favoriteUpdate.put("favorite", true);

        MvcResult favoriteResult = mockMvc.perform(patch("/api/v1/journals/{id}", created.id())
                        .header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(favoriteUpdate)))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"1\""))
                .andExpect(jsonPath("$.journalVersion").value(1))
                .andReturn();
        JsonNode favoriteBody = body(favoriteResult);
        String analysisUpdate = """
                {"version":%d,"contentText":"Changed analysis input","stressScore":null}
                """.formatted(favoriteBody.get("version").asLong());

        mockMvc.perform(patch("/api/v1/journals/{id}", created.id())
                        .header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(analysisUpdate))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"2\""))
                .andExpect(jsonPath("$.journalVersion").value(2))
                .andExpect(jsonPath("$.stressScore").doesNotExist());

        mockMvc.perform(patch("/api/v1/journals/{id}", created.id())
                        .header("Authorization", bearer(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("version", 1, "favorite", false))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("JOURNAL_VERSION_CONFLICT"));
    }

    @Test
    void ownerScopingReturnsNotFoundForAnotherUser() throws Exception {
        Session owner = register("owner@example.com");
        Session stranger = register("stranger@example.com");
        Created created = create(owner, journalBody("Private", 5), null);

        mockMvc.perform(get("/api/v1/journals/{id}", created.id())
                        .header("Authorization", bearer(stranger)))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/v1/journals/{id}", created.id())
                        .header("Authorization", bearer(stranger))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("version", 0, "favorite", true))))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/journals/{id}", created.id())
                        .header("Authorization", bearer(stranger)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createIsDurablyIdempotentAndRejectsKeyReuseWithDifferentBody() throws Exception {
        Session session = register("idempotency@example.com");
        String key = "journal-create-001";
        Map<String, Object> original = journalBody("Idempotent", 7);

        Created first = create(session, original, key);
        Created replay = create(session, original, key);
        assertThat(replay.id()).isEqualTo(first.id());
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM journal_entries WHERE user_id = ?",
                        Integer.class,
                        session.userId()))
                .isEqualTo(1);

        mockMvc.perform(post("/api/v1/journals")
                        .header("Authorization", bearer(session))
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(journalBody("Different request", 7))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
    }

    @Test
    void historyP95StaysBelowTheTwoSecondMvpTargetWithOneThousandEntries() throws Exception {
        Session session = register("performance@example.com");
        Instant now = Instant.now();
        List<Object[]> batch = new ArrayList<>(1_000);
        for (int index = 0; index < 1_000; index++) {
            Instant timestamp = now.minusSeconds(index);
            batch.add(new Object[] {
                UUID.randomUUID(),
                session.userId(),
                "Seed " + index,
                "Seed journal content " + index,
                Timestamp.from(timestamp),
                LocalDate.of(2026, 9, 21),
                Timestamp.from(timestamp),
                Timestamp.from(timestamp)
            });
        }
        jdbcTemplate.batchUpdate(
                """
                INSERT INTO journal_entries (
                    id, user_id, title, content_text, content_format, mood_score,
                    status, journal_version, occurred_at, entry_date, timezone_at_entry,
                    is_favorite, created_at, updated_at, version)
                VALUES (?, ?, ?, ?, 'PLAIN_TEXT', 5, 'SAVED', 1, ?, ?, 'UTC', FALSE, ?, ?, 0)
                """,
                batch);

        mockMvc.perform(get("/api/v1/journals")
                        .header("Authorization", bearer(session))
                        .queryParam("limit", "20"))
                .andExpect(status().isOk());

        List<Long> durations = new ArrayList<>(20);
        for (int request = 0; request < 20; request++) {
            long started = System.nanoTime();
            mockMvc.perform(get("/api/v1/journals")
                            .header("Authorization", bearer(session))
                            .queryParam("limit", "20"))
                    .andExpect(status().isOk());
            durations.add((System.nanoTime() - started) / 1_000_000);
        }
        Collections.sort(durations);
        long p95Milliseconds = durations.get(18);
        assertThat(p95Milliseconds).isLessThan(2_000L);
    }

    private Session register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", email,
                                "password", PASSWORD,
                                "displayName", "Journal User",
                                "acceptTerms", true,
                                "acceptPrivacy", true))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = body(result);
        return new Session(
                UUID.fromString(response.get("user").get("id").asText()),
                response.get("accessToken").asText());
    }

    private Created create(Session session, Map<String, Object> request, String key) throws Exception {
        var builder = post("/api/v1/journals")
                .header("Authorization", bearer(session))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request));
        if (key != null) {
            builder.header("Idempotency-Key", key);
        }
        MvcResult result = mockMvc.perform(builder)
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = body(result);
        return new Created(UUID.fromString(response.get("id").asText()), response);
    }

    private Map<String, Object> journalBody(String content, int mood) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("contentText", content);
        request.put("contentFormat", "PLAIN_TEXT");
        request.put("moodScore", mood);
        request.put("stressScore", 6);
        request.put("occurredAt", "2026-09-20T18:30:00Z");
        request.put("timezoneAtEntry", "Asia/Ho_Chi_Minh");
        request.put("favorite", false);
        return request;
    }

    private String bearer(Session session) {
        return "Bearer " + session.accessToken();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String json(Object value) {
        return objectMapper.writeValueAsString(value);
    }

    private record Session(UUID userId, String accessToken) {}
    private record Created(UUID id, JsonNode body) {}
}
