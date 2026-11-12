package com.mylog.insight;

import com.mylog.TestcontainersConfiguration;
import com.mylog.identity.application.VerificationDelivery;
import com.mylog.insight.application.InsightNarrator;
import com.mylog.insight.application.InsightService;
import com.mylog.insight.domain.PatternDetectors;
import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.safety.application.SafetyDecision;
import com.mylog.safety.application.SafetyScreeningUseCase;
import com.mylog.user.application.UserProfileUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Last-30-day pattern insights on PostgreSQL with synthetic data and a fake AI narrator. */
@Import({TestcontainersConfiguration.class, InsightPatternIntegrationTest.Fixtures.class})
@SpringBootTest(properties = {"mylog.identity.enabled=true", "spring.mail.host=localhost",
        "MYLOG_MAIL_FROM=noreply@mylog.local", "logging.level.root=WARN"})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class InsightPatternIntegrationTest {
    static final Instant NOW = Instant.parse("2026-11-11T10:00:00Z");
    static final LocalDate TODAY = LocalDate.of(2026, 11, 11);
    static final AtomicReference<Boolean> AI_ON = new AtomicReference<>(false);

    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    static class Fixtures {
        @Bean @Primary Clock patternClock() { return Clock.fixed(NOW, ZoneId.of("UTC")); }

        /** Rewords WEEKDAY acceptably and every other type with forbidden wording, when AI_ON is set. */
        @Bean InsightNarrator fakeNarrator() {
            return requests -> {
                Map<String, String> out = new HashMap<>();
                if (AI_ON.get()) for (var r : requests) out.put(r.key(), r.type().equals(PatternDetectors.WEEKDAY)
                        ? "Thứ Hai có vẻ là ngày nặng nhất tuần của bạn. Nhẹ nhàng với bản thân vào đầu tuần nhé."
                        : "Điều này xảy ra vì bạn có dấu hiệu trầm cảm.");
                return new InsightNarrator.Result(out, "openai", "fake-model", "test-v1", 10, 5, BigDecimal.ZERO, 3, true);
            };
        }
    }

    @Autowired InsightService insights;
    @Autowired com.mylog.insight.application.DashboardService dashboard;
    @Autowired UserProfileUseCase users;
    @Autowired SensitiveDataCipher cipher;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @MockitoBean VerificationDelivery delivery;
    @MockitoBean SafetyScreeningUseCase screening;

    @BeforeEach void allowOutput() {
        when(screening.screen(anyString())).thenReturn(new SafetyDecision(null, "ALLOW", "r", "c", "c", "p", BigDecimal.ONE, "s"));
        AI_ON.set(false);
    }

    @Test void refreshFindsPatternsInTheLastThirtyDaysOnlyAndIsOwnerScoped() {
        UUID owner = user();
        UUID stranger = user();
        seedThirtyDays(owner);
        // Data older than the window must not influence anything.
        insertEntry(owner, TODAY.minusDays(45), 1, 10, 600, null);

        var page = insights.refreshRecent(owner);
        Set<String> types = page.items().stream().map(i -> i.type()).collect(Collectors.toSet());
        assertTrue(types.contains(PatternDetectors.WEEKDAY), types.toString());
        assertTrue(types.contains(PatternDetectors.EMOTION_TOPIC), types.toString());
        assertTrue(page.items().size() <= 5);
        page.items().forEach(i -> {
            assertEquals(InsightService.PATTERN_VERSION, i.algorithmVersion());
            assertEquals(TODAY.minusDays(29), i.from());
            assertEquals(TODAY, i.to());
            assertEquals("STATISTICAL", i.generatedBy(), "no AI_PROCESSING consent yet");
            assertFalse(i.evidence().isEmpty());
        });
        var weekday = page.items().stream().filter(i -> i.type().equals(PatternDetectors.WEEKDAY)).findFirst().orElseThrow();
        assertTrue(weekday.narrative().contains("thứ Hai"), weekday.narrative());
        assertTrue(insights.list(stranger, null, null, null, 20).items().isEmpty());

        byte[] stored = jdbc.queryForObject("select encrypted_narrative from insights where id=?", byte[].class, weekday.id());
        assertFalse(new String(stored, StandardCharsets.UTF_8).contains("thứ Hai"), "narrative is encrypted");

        // Refreshing again replaces rather than duplicates.
        int active = page.items().size();
        insights.refreshRecent(owner);
        assertEquals(active, jdbc.queryForObject(
                "select count(*) from insights where user_id=? and status='ACTIVE' and algorithm_version=?",
                Integer.class, owner, InsightService.PATTERN_VERSION));
    }

    @Test void aiWordingNeedsConsentAndPassesValidationOtherwiseTemplatesStay() {
        UUID owner = user();
        seedThirtyDays(owner);
        AI_ON.set(true);
        assertTrue(insights.refreshRecent(owner).items().stream().allMatch(i -> i.generatedBy().equals("STATISTICAL")),
                "without AI_PROCESSING consent the provider is not used");
        // The fixed test clock gives both decisions the same timestamp; drop the default refusal first.
        jdbc.update("delete from user_consents where user_id=? and consent_type='AI_PROCESSING'", owner);
        users.decideConsent(owner, "AI_PROCESSING", "v1", true);
        var page = insights.refreshRecent(owner);
        var weekday = page.items().stream().filter(i -> i.type().equals(PatternDetectors.WEEKDAY)).findFirst().orElseThrow();
        assertEquals("AI_ASSISTED", weekday.generatedBy());
        assertTrue(weekday.narrative().startsWith("Thứ Hai có vẻ"));
        page.items().stream().filter(i -> !i.type().equals(PatternDetectors.WEEKDAY)).forEach(i -> {
            assertEquals("STATISTICAL", i.generatedBy());
            assertFalse(i.narrative().contains("trầm cảm"));
        });
        assertTrue(jdbc.queryForObject("select count(*) from ai_usage_records where operation='INSIGHT_NARRATIVE'",
                Integer.class) > 0);
    }

    @Test void dashboardSpectrumUsesPickedIconsAndOnlyPrimaryTones() {
        UUID owner = user();
        UUID happy = insertEntry(owner, TODAY.minusDays(1), 8, 3, 420, null);
        UUID calm = insertEntry(owner, TODAY.minusDays(2), 6, 4, 420, null);
        jdbc.update("update journal_entries set mood_code='happy' where id=?", happy);
        attachAnalysis(owner, happy, List.of("JOY", "HOPE"), "SELF_CARE");
        attachAnalysis(owner, calm, List.of("CALM"), "WORK");
        var view = dashboard.get(owner, "30d");
        assertEquals(Map.of("JOY", 1L, "CALM", 1L), view.topEmotions().stream()
                .collect(Collectors.toMap(c -> c.code(), c -> c.count())), "secondary HOPE is not counted");
        assertEquals(Map.of("happy", 1L, "neutral", 1L), view.topMoods().stream()
                .collect(Collectors.toMap(c -> c.code(), c -> c.count())));
    }

    @Test void hiddenInsightStaysHiddenAndEndpointsRequireOwner() throws Exception {
        UUID owner = user();
        UUID stranger = user();
        seedThirtyDays(owner);
        mvc.perform(post("/api/v1/insights:refresh")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/insights:refresh").with(jwt().jwt(j -> j.subject(owner.toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].generatedBy").value("STATISTICAL"));
        UUID id = jdbc.queryForObject("select id from insights where user_id=? and status='ACTIVE' and insight_type=?",
                UUID.class, owner, PatternDetectors.WEEKDAY);
        mvc.perform(patch("/api/v1/insights/{id}", id).with(jwt().jwt(j -> j.subject(stranger.toString())))
                        .contentType("application/json").content("{\"status\":\"HIDDEN\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/v1/insights/{id}", id).with(jwt().jwt(j -> j.subject(owner.toString())))
                        .contentType("application/json").content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/insights/{id}", id).with(jwt().jwt(j -> j.subject(owner.toString())))
                        .contentType("application/json").content("{\"status\":\"HIDDEN\"}"))
                .andExpect(status().isNoContent());
        var page = insights.refreshRecent(owner);
        assertTrue(page.items().stream().noneMatch(i -> i.type().equals(PatternDetectors.WEEKDAY)));
    }

    /** 30 days: Mondays are stressful; work entries carry ANXIETY, others CALM. */
    private void seedThirtyDays(UUID userId) {
        for (int i = 0; i < 30; i++) {
            LocalDate day = TODAY.minusDays(i);
            boolean monday = day.getDayOfWeek() == DayOfWeek.MONDAY;
            boolean work = i % 3 == 0 || monday;
            UUID entry = insertEntry(userId, day, monday ? 5 : 7, monday ? 8 : 4, 420, null);
            attachAnalysis(userId, entry, work ? List.of("ANXIETY") : List.of("CALM"), work ? "WORK" : "SELF_CARE");
        }
    }

    private UUID insertEntry(UUID userId, LocalDate day, int mood, int stress, int sleep, String risk) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into journal_entries(id,user_id,encrypted_payload,payload_iv,wrapped_data_key,
                    encryption_key_version,content_format_version,occurred_at,local_date,timezone,
                    mood_code,mood_score,stress_score,energy_score,sleep_minutes,entry_status,
                    risk_level,analysis_status,content_version,created_at,updated_at)
                values (?,?,decode('00','hex'),decode('00','hex'),decode('00','hex'),'synthetic',1,?,?,
                    'Asia/Ho_Chi_Minh','neutral',?,?,6,?,'SAVED',?,'PENDING',1,?,?)
                """, id, userId, Timestamp.from(day.atTime(20, 0).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant()),
                day, BigDecimal.valueOf(mood), BigDecimal.valueOf(stress), sleep, risk == null ? "NORMAL" : risk,
                Timestamp.from(NOW), Timestamp.from(NOW));
        return id;
    }

    private void attachAnalysis(UUID userId, UUID entryId, List<String> emotions, String topic) {
        UUID analysisId = UUID.randomUUID();
        var encrypted = cipher.encrypt("ai_analyses.output", userId, analysisId, "{\"reflection\":\"Synthetic\"}");
        jdbc.update("""
                insert into ai_analyses(id,journal_entry_id,user_id,content_version,analysis_version,status,
                    sentiment_label,sentiment_score,encrypted_output,output_iv,wrapped_data_key,
                    encryption_key_version,output_schema_version,provider,model,prompt_template_version,
                    safety_policy_version,started_at,completed_at,created_at)
                values (?,?,?,1,1,'SUCCEEDED','NEUTRAL',0.5,?,?,?,?,1,'LOCAL_FAKE','test','test-v1','test-policy',?,?,?)
                """, analysisId, entryId, userId, encrypted.ciphertext(), encrypted.iv(), encrypted.wrappedKey(),
                encrypted.keyVersion(), Timestamp.from(NOW), Timestamp.from(NOW), Timestamp.from(NOW));
        jdbc.update("update journal_entries set latest_analysis_id=?,analysis_status='ANALYZED' where id=?", analysisId, entryId);
        short rank = 1;
        for (String emotion : emotions)
            jdbc.update("insert into analysis_emotions(analysis_id,emotion_code,score,rank) values (?,?,0.8,?)",
                    analysisId, emotion, rank++);
        jdbc.update("insert into analysis_topics(id,analysis_id,topic_code,score,rank) values (?,?,?,0.8,1)",
                UUID.randomUUID(), analysisId, topic);
    }

    private UUID user() {
        UUID id = UUID.randomUUID();
        String email = "pattern-" + id + "@example.test";
        var encrypted = cipher.encrypt("users.email", id, id, email);
        jdbc.update("""
                insert into users(id,email_lookup_hash,encrypted_email,email_iv,email_wrapped_key,email_key_version,
                    password_hash,auth_provider,status,created_at,updated_at)
                values (?,?,?,?,?,?,'test','LOCAL','ACTIVE',?,?)
                """, id, cipher.lookupHash(email), encrypted.ciphertext(), encrypted.iv(),
                encrypted.wrappedKey(), encrypted.keyVersion(), Timestamp.from(NOW), Timestamp.from(NOW));
        users.createDefault(id, "Asia/Ho_Chi_Minh", "vi", "v1", "v1", NOW);
        return id;
    }
}
