package com.mylog.insight;

import com.mylog.TestcontainersConfiguration;
import com.mylog.analysis.application.PatternExtractionService;
import com.mylog.identity.application.VerificationDelivery;
import com.mylog.insight.application.ThinkingPatternService;
import com.mylog.journal.application.JournalService;
import com.mylog.journal.application.command.CreateJournalEntryCommand;
import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.selfcare.application.SelfCareService;
import com.mylog.user.application.UserProfileUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Thinking-pattern cards end to end: keyword extraction, the 30-day rules, the API and the plan. The entries
 * mirror seed-demo-journal.mjs --plant-patterns. Synthetic data only.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {"mylog.identity.enabled=true", "mylog.patterns.extractor=fake", "spring.mail.host=localhost",
        "MYLOG_MAIL_FROM=noreply@mylog.local", "logging.level.root=WARN"})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ThinkingPatternIntegrationTest {
    static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired PatternExtractionService extraction;
    @Autowired ThinkingPatternService patterns;
    @Autowired JournalService journal;
    @Autowired UserProfileUseCase users;
    @Autowired SelfCareService selfCare;
    @Autowired SensitiveDataCipher cipher;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @Autowired MockMvc mvc;
    @MockitoBean VerificationDelivery delivery;

    @Test void onlyARepeatedPatternSpreadOverWeeksBecomesACard() throws Exception {
        UUID user = plantedUser();
        String body = mvc.perform(post("/api/v1/patterns:refresh").with(as(user)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode items = mapper.readTree(body).path("items");
        assertEquals(1, items.size(), "one new card per refresh");
        JsonNode card = items.get(0);
        assertEquals("CATASTROPHIZING", card.path("code").asText());
        assertEquals(4, card.path("entryCount").asInt());
        assertTrue(card.path("weekCount").asInt() >= 2);
        assertEquals(2, card.path("quotes").size(), "two quotes in the user's own words");
        card.path("quotes").forEach(q -> assertTrue(q.path("text").asText().contains("coi như hết")));
        assertEquals(3, card.path("steps").size());
        assertFalse(card.path("contentReviewed").asBoolean(), "draft texts until a knowledge article is approved");
        assertTrue(card.path("narrative").asText().endsWith("Điều này có đúng với bạn không?"));

        // MIND_READING (one week) and SHOULD_STATEMENTS (two entries) never qualify; a second refresh adds nothing.
        mvc.perform(post("/api/v1/patterns:refresh").with(as(user))).andExpect(jsonPath("$.items.length()").value(1));
        assertEquals(1, jdbc.queryForObject("select count(*) from pattern_recognitions where user_id=?", Integer.class, user));
        mvc.perform(get("/api/v1/patterns").with(as(user))).andExpect(jsonPath("$.items[0].code").value("CATASTROPHIZING"));
    }

    @Test void cardsFollowTheOptInAndAreOwnerScoped() throws Exception {
        UUID user = plantedUser();
        UUID card = patterns.refresh(user).getFirst().id();
        UUID stranger = user();
        mvc.perform(get("/api/v1/patterns").with(as(stranger))).andExpect(jsonPath("$.items.length()").value(0));
        mvc.perform(patch("/api/v1/patterns/" + card).with(as(stranger)).contentType("application/json")
                .content("{\"status\":\"HIDDEN\"}")).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/patterns/" + card + ":start-plan").with(as(stranger))).andExpect(status().isNotFound());

        users.decideConsent(user, "THINKING_PATTERNS", "v1", false);
        mvc.perform(get("/api/v1/patterns").with(as(user))).andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test void notReallyRetiresTheCardAndKeepsItAwayForSixtyDays() throws Exception {
        UUID user = plantedUser();
        UUID card = patterns.refresh(user).getFirst().id();
        mvc.perform(patch("/api/v1/patterns/" + card).with(as(user)).contentType("application/json")
                .content("{\"feedback\":\"NOT_REALLY\",\"reason\":\"WRONG_QUOTE\"}")).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/patterns").with(as(user))).andExpect(jsonPath("$.items.length()").value(0));
        jdbc.update("update pattern_recognitions set created_at=created_at - interval '8 days' where id=?", card);
        assertTrue(patterns.refresh(user).isEmpty(), "not shown again within 60 days");
        assertEquals("WRONG_QUOTE", jdbc.queryForObject("select feedback_reason from pattern_recognitions where id=?",
                String.class, card));
    }

    @Test void requestsMustChooseFeedbackOrHide() throws Exception {
        UUID user = plantedUser();
        UUID card = patterns.refresh(user).getFirst().id();
        for (String bad : List.of("{}", "{\"feedback\":\"CONFIRMED\",\"status\":\"HIDDEN\"}",
                "{\"feedback\":\"CONFIRMED\",\"reason\":\"WRONG_QUOTE\"}", "{\"feedback\":\"MAYBE\"}"))
            mvc.perform(patch("/api/v1/patterns/" + card).with(as(user)).contentType("application/json").content(bad))
                    .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/patterns/" + card).with(as(user)).contentType("application/json")
                .content("{\"status\":\"HIDDEN\"}")).andExpect(status().isNoContent());
        jdbc.update("update pattern_recognitions set created_at=created_at - interval '8 days' where id=?", card);
        assertTrue(patterns.refresh(user).isEmpty(), "a hidden pattern is not offered again");
    }

    @Test void startingThePlanCreatesAGoalOnce() throws Exception {
        UUID user = plantedUser();
        UUID card = patterns.refresh(user).getFirst().id();
        mvc.perform(patch("/api/v1/patterns/" + card).with(as(user)).contentType("application/json")
                .content("{\"feedback\":\"CONFIRMED\"}")).andExpect(status().isNoContent());
        String first = mvc.perform(post("/api/v1/patterns/" + card + ":start-plan").with(as(user)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.created").value(true))
                .andReturn().getResponse().getContentAsString();
        String goalId = mapper.readTree(first).path("goalId").asText();
        mvc.perform(post("/api/v1/patterns/" + card + ":start-plan").with(as(user)))
                .andExpect(jsonPath("$.created").value(false)).andExpect(jsonPath("$.goalId").value(goalId));
        var goal = selfCare.goals(user).stream().filter(g -> g.id().toString().equals(goalId)).findFirst().orElseThrow();
        assertEquals("MINDFULNESS", goal.category());
        assertEquals(1, goal.habits().size());
        assertTrue(goal.description().startsWith("Tuần 1:"));
        mvc.perform(get("/api/v1/patterns").with(as(user)))
                .andExpect(jsonPath("$.items[0].feedback").value("CONFIRMED"))
                .andExpect(jsonPath("$.items[0].planGoalId").value(goalId));
    }

    @Test void aHighRiskEntryInTheWindowRemovesTheCards() throws Exception {
        UUID user = plantedUser();
        assertEquals(1, patterns.refresh(user).size());
        UUID risky = entry(user, LocalDate.now(ZONE).minusDays(1), "Một ngày rất nặng nề.");
        jdbc.update("update journal_entries set risk_level='HIGH' where id=?", risky);
        assertTrue(patterns.refresh(user).isEmpty());
        assertEquals(0, jdbc.queryForObject("select count(*) from pattern_recognitions where user_id=? and status='ACTIVE'",
                Integer.class, user));
    }

    @Test void exportListsCardsWithoutQuotes() throws Exception {
        UUID user = plantedUser();
        patterns.refresh(user);
        var exported = patterns.exportAll(user);
        assertEquals(1, exported.size());
        assertEquals("CATASTROPHIZING", exported.getFirst().get("code"));
        assertFalse(exported.getFirst().toString().contains("coi như hết"), "no quotes in the export");
    }

    // ---- fixtures ------------------------------------------------------------------------------

    /** Same shape as --plant-patterns: one qualifying code and two that must not qualify. */
    private UUID plantedUser() throws Exception {
        UUID user = user();
        LocalDate today = LocalDate.now(ZONE);
        int i = 0;
        for (int days : new int[] {2, 9, 16, 23})
            entry(user, today.minusDays(days), "Hôm nay khá bận. Lỡ việc số " + (++i) + " hỏng thì coi như hết.");
        LocalDate monday = today.minusWeeks(2).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        for (int d = 0; d < 3; d++)
            entry(user, monday.plusDays(d), "Chắc cả nhóm nghĩ mình làm chậm lần " + d + ".");
        entry(user, today.minusDays(5), "Đáng lẽ mình phải xong hết việc trước khi được nghỉ.");
        entry(user, today.minusDays(18), "Đáng lẽ mình không được thấy mệt mới phải.");
        entry(user, today.minusDays(3), "Tối nay mình nấu canh chua.");
        users.decideConsent(user, "AI_PROCESSING", "v1", true);
        users.decideConsent(user, "THINKING_PATTERNS", "v1", true);
        var summary = extraction.extractRecent(user);
        assertEquals(10, summary.read(), summary.toString());
        return user;
    }

    private UUID entry(UUID user, LocalDate day, String text) throws Exception {
        var content = mapper.readTree(mapper.writeValueAsString(Map.of("type", "doc", "content", List.of(
                Map.of("type", "paragraph", "content", List.of(Map.of("type", "text", "text", text)))))));
        Instant at = day.atTime(LocalTime.of(20, 0)).atZone(ZONE).toInstant();
        UUID id = journal.create(user, new CreateJournalEntryCommand("Nhật ký thử", content, null, at,
                ZONE.getId(), null, null, null, null, null), "tp-" + UUID.randomUUID()).id();
        jdbc.update("update journal_entries set risk_level='NORMAL' where id=?", id);
        return id;
    }

    private UUID user() {
        UUID id = UUID.randomUUID();
        String email = "cards-" + id + "@example.test";
        var encrypted = cipher.encrypt("users.email", id, id, email);
        jdbc.update("""
                insert into users(id,email_lookup_hash,encrypted_email,email_iv,email_wrapped_key,email_key_version,
                    password_hash,auth_provider,status,created_at,updated_at)
                values (?,?,?,?,?,?,'test','LOCAL','ACTIVE',now(),now())
                """, id, cipher.lookupHash(email), encrypted.ciphertext(), encrypted.iv(),
                encrypted.wrappedKey(), encrypted.keyVersion());
        users.createDefault(id, ZONE.getId(), "vi", "v1", "v1", Instant.now().minusSeconds(60));
        return id;
    }

    private static RequestPostProcessor as(UUID user) {
        return jwt().jwt(j -> j.subject(user.toString()));
    }
}
