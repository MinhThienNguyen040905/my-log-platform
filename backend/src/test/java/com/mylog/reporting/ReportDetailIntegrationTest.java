package com.mylog.reporting;

import com.mylog.TestcontainersConfiguration;
import com.mylog.identity.application.VerificationDelivery;
import com.mylog.insight.application.InsightService;
import com.mylog.journal.application.JournalService;
import com.mylog.journal.application.command.CreateJournalEntryCommand;
import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.platform.web.ResourceNotFoundException;
import com.mylog.reporting.application.ReportService;
import com.mylog.reporting.infrastructure.ReportWorker;
import com.mylog.user.application.UserProfileUseCase;
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
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Detailed weekly reflection, on-demand creation and the nightly pattern refresh, with synthetic data. */
@Import({TestcontainersConfiguration.class, ReportDetailIntegrationTest.FixedClock.class})
@SpringBootTest(properties = {"mylog.identity.enabled=true", "mylog.reports.enabled=true",
        "mylog.reports.poll-delay-ms=600000", "mylog.reports.schedule-delay-ms=600000",
        "spring.mail.host=localhost", "MYLOG_MAIL_FROM=noreply@mylog.local", "logging.level.root=WARN"})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ReportDetailIntegrationTest {
    /** Wednesday 2026-11-11, 17:00 in Asia/Ho_Chi_Minh: the last finished week is 2026-11-02..08. */
    static final Instant NOW = Instant.parse("2026-11-11T10:00:00Z");

    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    static class FixedClock {
        @Bean @Primary Clock reportDetailClock() { return Clock.fixed(NOW, ZoneId.of("UTC")); }
    }

    @Autowired ReportService reports;
    @Autowired ReportWorker worker;
    @Autowired InsightService insights;
    @Autowired JournalService journal;
    @Autowired UserProfileUseCase users;
    @Autowired SensitiveDataCipher cipher;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean VerificationDelivery delivery;

    @Test void onDemandWeeklyReportHasComparisonHighlightsAndOwnerOnlyTitles() throws Exception {
        UUID owner = user();
        UUID stranger = user();
        LocalDate previousMonday = LocalDate.of(2026, 10, 26);
        for (int i = 0; i < 7; i++) entry(owner, previousMonday.plusDays(i), 5, "Synthetic earlier " + i);
        LocalDate monday = LocalDate.of(2026, 11, 2);
        int[] moods = {6, 7, 6, 8, 7, 9, 7};
        for (int i = 0; i < 7; i++) entry(owner, monday.plusDays(i), moods[i], "Synthetic week day " + i);

        String body = mvc.perform(post("/api/v1/reports").with(jwt().jwt(j -> j.subject(owner.toString())))
                        .contentType("application/json").content("{\"type\":\"WEEKLY\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.from").value("2026-11-02")).andExpect(jsonPath("$.to").value("2026-11-08"))
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(mapper.readTree(body).get("id").asText());
        mvc.perform(post("/api/v1/reports").with(jwt().jwt(j -> j.subject(owner.toString())))
                .contentType("application/json").content("{\"type\":\"DAILY\"}")).andExpect(status().isBadRequest());

        worker.poll();
        var report = reports.get(owner, id);
        assertEquals("READY", report.status());
        assertEquals("RULE", report.generatedBy(), "no AI_PROCESSING consent");
        Map<?, ?> previous = (Map<?, ?>) report.metrics().get("previous");
        assertEquals(0, new BigDecimal("5.00").compareTo(new BigDecimal(previous.get("averageMood").toString())));
        assertEquals(7, ((Number) report.metrics().get("journalDays")).intValue());
        Map<?, ?> best = (Map<?, ?>) report.metrics().get("bestDay");
        assertEquals("2026-11-07", best.get("date"));
        String bestEntry = best.get("entryId").toString();
        assertEquals("Synthetic week day 5", report.entryTitles().get(bestEntry));
        assertTrue(report.narrative().contains("nhích lên so với tuần trước (5)"), report.narrative());
        assertTrue(report.narrative().contains("thứ Bảy 7/11 (9)"), report.narrative());
        String stored = jdbc.queryForObject("select metrics_snapshot::text from reports where id=?", String.class, id);
        assertFalse(stored.contains("Synthetic"), "titles are never stored in the plaintext snapshot");
        assertThrows(ResourceNotFoundException.class, () -> reports.get(stranger, id));

        // Asking again returns the same report; regenerate makes a new immutable version.
        assertEquals(id, reports.requestLatest(owner, "WEEKLY").id());
        mvc.perform(post("/api/v1/reports/{id}:regenerate", id).with(jwt().jwt(j -> j.subject(stranger.toString()))))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/reports/{id}:regenerate", id).with(jwt().jwt(j -> j.subject(owner.toString()))))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.version").value(2));
    }

    @Test void nightlyRefreshRunsOncePerLocalDayAndOnlyWithNewData() {
        UUID owner = user();
        // Sleep and mood move together, so the sleep–mood pattern is found and stored.
        for (int i = 0; i < 10; i++) entry(owner, LocalDate.of(2026, 11, 11).minusDays(i), 5 + i % 3, "Synthetic " + i, 300 + (i % 3) * 90);
        int localHour = NOW.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).getHour();
        assertTrue(insights.refreshDueUsers(localHour, false) >= 1);
        String firstRun = lastRefresh(owner);
        assertNotNull(firstRun, "pattern insights were stored");
        assertTrue(jdbc.queryForObject("select bool_and(generated_by='STATISTICAL') from insights where user_id=? and algorithm_version=?",
                Boolean.class, owner, InsightService.PATTERN_VERSION), "scheduled run does not use AI wording");
        insights.refreshDueUsers(localHour, false);
        assertEquals(firstRun, lastRefresh(owner), "already refreshed today: not recomputed");
        insights.refreshDueUsers((localHour + 1) % 24, false);
        assertEquals(firstRun, lastRefresh(owner), "not this user's hour");
    }

    private String lastRefresh(UUID owner) {
        return jdbc.queryForObject("select max(created_at)::text from insights where user_id=? and algorithm_version=?",
                String.class, owner, InsightService.PATTERN_VERSION);
    }

    private void entry(UUID userId, LocalDate day, int mood, String title) {
        entry(userId, day, mood, title, 420);
    }

    private void entry(UUID userId, LocalDate day, int mood, String title, int sleep) {
        var content = mapper.readTree("""
                {"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"Synthetic day"}]}]}
                """);
        journal.create(userId, new CreateJournalEntryCommand(title, content, null,
                day.atTime(20, 0).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(), "Asia/Ho_Chi_Minh", "neutral",
                BigDecimal.valueOf(mood), BigDecimal.valueOf(4), BigDecimal.valueOf(6), sleep, null),
                "report-" + UUID.randomUUID());
    }

    private UUID user() {
        UUID id = UUID.randomUUID();
        String email = "report-" + id + "@example.test";
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
