package com.mylog.reporting;

import com.mylog.TestcontainersConfiguration;
import com.mylog.identity.application.VerificationDelivery;
import com.mylog.insight.application.DashboardService;
import com.mylog.insight.application.InsightService;
import com.mylog.journal.application.JournalService;
import com.mylog.journal.application.command.CreateJournalEntryCommand;
import com.mylog.platform.crypto.SensitiveDataCipher;
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
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({TestcontainersConfiguration.class, M4IntegrationTest.FixedClock.class})
@SpringBootTest(properties = {"mylog.identity.enabled=true", "mylog.reports.enabled=true",
        "mylog.reports.poll-delay-ms=600000", "mylog.reports.schedule-delay-ms=600000",
        "spring.mail.host=localhost", "MYLOG_MAIL_FROM=noreply@mylog.local",
        "logging.level.root=WARN"})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class M4IntegrationTest {
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    static class FixedClock {
        @Bean @Primary Clock m4Clock() {
            return Clock.fixed(Instant.parse("2026-10-04T18:00:00Z"), ZoneId.of("UTC"));
        }
    }

    @Autowired DashboardService dashboard;
    @Autowired InsightService insights;
    @Autowired ReportService reports;
    @Autowired ReportWorker worker;
    @Autowired JournalService journal;
    @Autowired UserProfileUseCase users;
    @Autowired SensitiveDataCipher cipher;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean VerificationDelivery delivery;

    @Test void dashboardInsightAndVersionedWeeklyReportAreOwnerScoped() throws Exception {
        UUID owner = user();
        UUID stranger = user();
        LocalDate start = LocalDate.of(2026, 9, 29);
        UUID firstEntry = null;
        for (int i = 0; i < 7; i++) {
            LocalDate day = start.plusDays(i);
            UUID entry = journal.create(owner, command(day, i + 2, 330 + i * 30),
                    "m4-create-" + UUID.randomUUID()).id();
            if (i == 0) firstEntry = entry;
        }
        insertCheckin(owner, LocalDate.of(2026, 10, 1), 9, 300);
        attachSyntheticAnalysis(owner, firstEntry);

        var dashboardView = dashboard.get(owner, "7d");
        assertEquals(7, dashboardView.timeline().size());
        assertEquals(7, dashboardView.journalEntryCount());
        assertEquals(7, dashboardView.currentJournalStreak());
        var preferred = dashboardView.timeline().stream()
                .filter(d -> d.date().equals(LocalDate.of(2026, 10, 1))).findFirst().orElseThrow();
        assertEquals("CHECKIN", preferred.source());
        assertEquals(0, preferred.moodScore().compareTo(BigDecimal.valueOf(9)));
        assertEquals(1, preferred.journalCount());
        assertEquals("CALM", dashboardView.topEmotions().getFirst().code());
        assertEquals("OTHER", dashboardView.topTopics().getFirst().code());
        assertTrue(dashboard.get(stranger, "7d").timeline().isEmpty());

        insights.generate(owner, start, start.plusDays(6));
        insights.generate(owner, start, start.plusDays(6));
        var insightPage = insights.list(owner, null, null, null, 20);
        assertEquals(1, insightPage.items().size());
        assertEquals(7, insightPage.items().getFirst().sampleSize());
        assertEquals(14, insightPage.items().getFirst().evidence().size());
        assertTrue(insightPage.items().getFirst().narrative().contains("không chứng minh nguyên nhân"));
        assertTrue(insights.list(stranger, null, null, null, 20).items().isEmpty());
        insights.generate(owner, start.minusDays(1), start.plusDays(6));
        var firstInsightPage = insights.list(owner, null, null, null, 1);
        assertNotNull(firstInsightPage.nextCursor());
        assertEquals(1, insights.list(owner, null, null, firstInsightPage.nextCursor(), 1).items().size());
        byte[] encrypted = jdbc.queryForObject("select encrypted_narrative from insights where user_id=? and period_start=?", byte[].class, owner, start);
        assertFalse(new String(encrypted, StandardCharsets.UTF_8).contains("mối liên hệ"));

        reports.scheduleDue();
        reports.scheduleDue();
        assertEquals(1, jdbc.queryForObject("select count(*) from reports where user_id=? and report_type='WEEKLY'", Integer.class, owner));
        worker.poll();
        var first = reports.list(owner, "WEEKLY", null, 20).items().getFirst();
        assertEquals("READY", first.status());
        assertEquals(1, first.version());
        assertEquals(6, first.sampleSize());
        assertFalse(reports.get(owner, first.id()).evidence().isEmpty());
        assertThrows(com.mylog.platform.web.ResourceNotFoundException.class,
                () -> reports.get(stranger, first.id()));
        Object originalMood = first.metrics().get("averageMood");
        jdbc.update("update daily_checkins set mood_score=1 where user_id=? and local_date=?",
                owner, LocalDate.of(2026, 10, 1));
        UUID regeneratedId = reports.regenerate(owner, first.id());
        worker.poll();
        var regenerated = reports.get(owner, regeneratedId);
        assertEquals(2, regenerated.version());
        assertEquals("READY", regenerated.status());
        assertEquals(originalMood, reports.get(owner, first.id()).metrics().get("averageMood"));
        assertNotEquals(originalMood, regenerated.metrics().get("averageMood"));
        var firstReportPage = reports.list(owner, "WEEKLY", null, 1);
        assertNotNull(firstReportPage.nextCursor());
        assertEquals(1, reports.list(owner, "WEEKLY", firstReportPage.nextCursor(), 1).items().size());

        mvc.perform(get("/api/v1/dashboard?range=7d"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/insights")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/reports")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/dashboard?range=7d")
                .with(jwt().jwt(jwt -> jwt.subject(owner.toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.journalEntryCount").value(7));
        mvc.perform(get("/api/v1/dashboard?range=365d")
                .with(jwt().jwt(jwt -> jwt.subject(owner.toString()))))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/reports/{id}", first.id())
                .with(jwt().jwt(jwt -> jwt.subject(stranger.toString()))))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/insights")
                .with(jwt().jwt(jwt -> jwt.subject(owner.toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].sampleSize").value(7));
    }

    @Test void dashboardThirtyDaysHandlesTenThousandSyntheticEntries() {
        UUID owner = user();
        jdbc.update("""
                insert into journal_entries(id,user_id,encrypted_payload,payload_iv,wrapped_data_key,
                    encryption_key_version,content_format_version,occurred_at,local_date,timezone,
                    mood_code,mood_score,stress_score,energy_score,sleep_minutes,entry_status,
                    risk_level,analysis_status,content_version,created_at,updated_at)
                select gen_random_uuid(),?,decode('00','hex'),decode('00','hex'),decode('00','hex'),
                    'synthetic',1,timestamptz '2026-09-05 17:00:00+00' + ((g-1)%30)*interval '1 day',
                    date '2026-09-06' + ((g-1)%30),'Asia/Ho_Chi_Minh','calm',
                    1+(g%10),3,6,300+(g%180),'SAVED','NORMAL','NOT_REQUESTED',1,
                    timestamptz '2026-10-04 18:00:00+00',timestamptz '2026-10-04 18:00:00+00'
                from generate_series(1,10000) g
                """, owner);
        for (int i = 0; i < 3; i++) dashboard.get(owner, "30d");
        long[] millis = new long[30];
        for (int i = 0; i < millis.length; i++) {
            long start = System.nanoTime();
            var result = dashboard.get(owner, "30d");
            millis[i] = (System.nanoTime() - start) / 1_000_000;
            assertEquals(10_000, result.journalEntryCount());
            assertEquals(30, result.timeline().size());
        }
        Arrays.sort(millis);
        long p95 = millis[28];
        System.out.println("M4 dashboard 30d synthetic 10k local p95=" + p95 + "ms");
        assertTrue(p95 < 500, "Dashboard local p95 should meet initial 500 ms target: " + p95);
    }

    private CreateJournalEntryCommand command(LocalDate day, int mood, int sleep) {
        var content = mapper.readTree("""
                {"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"Synthetic day"}]}]}
                """);
        return new CreateJournalEntryCommand("Synthetic " + day, content, null,
                day.atTime(12, 0).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(),
                "Asia/Ho_Chi_Minh", "calm", BigDecimal.valueOf(mood),
                BigDecimal.valueOf(3), BigDecimal.valueOf(6), sleep);
    }

    private void insertCheckin(UUID userId, LocalDate day, int mood, int sleep) {
        Instant now = Instant.parse("2026-10-04T18:00:00Z");
        jdbc.update("""
                insert into daily_checkins(id,user_id,local_date,timezone,mood_code,mood_score,
                    stress_score,energy_score,sleep_minutes,source,created_at,updated_at)
                values (?,?,?,'Asia/Ho_Chi_Minh','calm',?,3,6,?,'USER',?,?)
                """, UUID.randomUUID(), userId, day, BigDecimal.valueOf(mood), sleep,
                Timestamp.from(now), Timestamp.from(now));
    }

    private void attachSyntheticAnalysis(UUID userId, UUID entryId) {
        UUID analysisId = UUID.randomUUID();
        var encrypted = cipher.encrypt("ai_analyses.output", userId, analysisId, "{\"reflection\":\"Synthetic\"}");
        Instant now = Instant.parse("2026-10-04T18:00:00Z");
        jdbc.update("""
                insert into ai_analyses(id,journal_entry_id,user_id,content_version,analysis_version,status,
                    sentiment_label,sentiment_score,encrypted_output,output_iv,wrapped_data_key,
                    encryption_key_version,output_schema_version,provider,model,prompt_template_version,
                    safety_policy_version,started_at,completed_at,created_at)
                values (?,?,?,1,1,'SUCCEEDED','NEUTRAL',0.5,?,?,?,?,1,'LOCAL_FAKE','test',
                    'test-v1','test-policy',?,?,?)
                """, analysisId, entryId, userId, encrypted.ciphertext(), encrypted.iv(),
                encrypted.wrappedKey(), encrypted.keyVersion(), Timestamp.from(now),
                Timestamp.from(now), Timestamp.from(now));
        jdbc.update("update journal_entries set latest_analysis_id=?,analysis_status='ANALYZED' where id=?",
                analysisId, entryId);
        jdbc.update("insert into analysis_emotions(analysis_id,emotion_code,score,rank) values (?,'CALM',0.8,1)", analysisId);
        jdbc.update("insert into analysis_topics(id,analysis_id,topic_code,score,rank) values (?,?,'OTHER',0.8,1)",
                UUID.randomUUID(), analysisId);
    }

    private UUID user() {
        UUID id = UUID.randomUUID();
        String email = "m4-" + id + "@example.test";
        var encrypted = cipher.encrypt("users.email", id, id, email);
        Instant now = Instant.parse("2026-10-04T18:00:00Z");
        jdbc.update("""
                insert into users(id,email_lookup_hash,encrypted_email,email_iv,email_wrapped_key,email_key_version,
                    password_hash,auth_provider,status,created_at,updated_at)
                values (?,?,?,?,?,?,'test','LOCAL','ACTIVE',?,?)
                """, id, cipher.lookupHash(email), encrypted.ciphertext(), encrypted.iv(),
                encrypted.wrappedKey(), encrypted.keyVersion(), Timestamp.from(now), Timestamp.from(now));
        users.createDefault(id, "Asia/Ho_Chi_Minh", "vi", "v1", "v1", now);
        return id;
    }
}
